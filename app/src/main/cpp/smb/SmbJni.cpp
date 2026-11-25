#include <jni.h>
#include <time.h>
#include <smb2/smb2.h>
#include <smb2/libsmb2.h>
#include <fcntl.h>
#include <cerrno>
#include <algorithm>
#include <cstdint>
#include <map>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

namespace {
class Utf {
 public:
  Utf(JNIEnv* env, jstring value) : env_(env), value_(value), text_(value ? env->GetStringUTFChars(value, nullptr) : nullptr) {}
  ~Utf() { if (text_) env_->ReleaseStringUTFChars(value_, text_); }
  std::string str() const { return text_ ? text_ : ""; }
 private:
  JNIEnv* env_;
  jstring value_;
  const char* text_;
};

void fail(JNIEnv* env, const std::string& message, int error = 0) {
  const char* klass = error == -ENOENT ? "java/io/FileNotFoundException" : "java/io/IOException";
  jclass cls = env->FindClass(klass);
  if (cls) { env->ThrowNew(cls, message.c_str()); env->DeleteLocalRef(cls); }
}

// A context is always used by exactly one thread at a time.
struct Session {
  smb2_context* ctx = nullptr;
  smb2fh* file = nullptr;
  uint64_t fileSize = 0;
  bool connected = false;
  std::mutex mutex;
  ~Session() {
    if (!ctx) return;
    if (file) smb2_close(ctx, file);
    if (connected) smb2_disconnect_share(ctx);
    smb2_destroy_context(ctx);
  }
};

std::unique_ptr<Session> connect(JNIEnv* env, jstring host, jstring share,
                                  jstring user, jstring password, jstring domain) {
  Utf h(env, host), s(env, share), u(env, user), p(env, password), d(env, domain);
  auto session = std::make_unique<Session>();
  session->ctx = smb2_init_context();
  if (!session->ctx) { fail(env, "Unable to create SMB context"); return nullptr; }
  smb2_set_timeout(session->ctx, 30);
  smb2_set_authentication(session->ctx, SMB2_SEC_NTLMSSP);
  smb2_set_user(session->ctx, u.str().c_str());
  smb2_set_password(session->ctx, p.str().c_str());
  smb2_set_domain(session->ctx, d.str().c_str());
  // Do not force sealing: still negotiate encryption when the server requires it.
  int rc = smb2_connect_share(session->ctx, h.str().c_str(), s.str().c_str(),
                              u.str().empty() ? nullptr : u.str().c_str());
  if (rc != 0) {
    const char* reason = smb2_get_error(session->ctx);
    fail(env, reason ? reason : "SMB connection failed", rc);
    return nullptr;
  }
  session->connected = true;
  return session;
}

// JNI NewStringUTF does not accept normal UTF-8 4-byte codepoints (e.g. emoji).
// Construct a Java String from UTF-8 bytes instead, preserving all filenames.
jstring stringFromUtf8(JNIEnv* env, const std::string& value) {
  jbyteArray bytes = env->NewByteArray(static_cast<jsize>(value.size()));
  if (!bytes) return nullptr;
  env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(value.size()),
                          reinterpret_cast<const jbyte*>(value.data()));
  jclass type = env->FindClass("java/lang/String");
  if (!type) { env->DeleteLocalRef(bytes); return nullptr; }
  jmethodID constructor = env->GetMethodID(type, "<init>", "([BLjava/lang/String;)V");
  jstring charset = env->NewStringUTF("UTF-8");
  jstring result = constructor && charset
      ? static_cast<jstring>(env->NewObject(type, constructor, bytes, charset)) : nullptr;
  if (charset) env->DeleteLocalRef(charset);
  env->DeleteLocalRef(type);
  env->DeleteLocalRef(bytes);
  return result;
}

jobject makeFile(JNIEnv* env, const std::string& name, const std::string& path,
                 bool directory, uint64_t size, uint64_t timestamp) {
  jclass type = env->FindClass("remix/myplayer/data/remote/RemoteFile");
  if (!type) return nullptr;
  jmethodID ctor = env->GetMethodID(type, "<init>", "(Ljava/lang/String;Ljava/lang/String;ZJJ)V");
  if (!ctor) { env->DeleteLocalRef(type); return nullptr; }
  jstring jname = stringFromUtf8(env, name);
  jstring jpath = stringFromUtf8(env, path);
  if (!jname || !jpath) {
    if (jname) env->DeleteLocalRef(jname);
    if (jpath) env->DeleteLocalRef(jpath);
    env->DeleteLocalRef(type);
    return nullptr;
  }
  jobject out = env->NewObject(type, ctor, jname, jpath,
                               static_cast<jboolean>(directory), static_cast<jlong>(size),
                               static_cast<jlong>(timestamp * 1000));
  env->DeleteLocalRef(jname);
  env->DeleteLocalRef(jpath);
  env->DeleteLocalRef(type);
  return out;
}

std::mutex handleMutex;
std::map<jlong, std::shared_ptr<Session>> handles;
jlong nextHandle = 1;

std::shared_ptr<Session> getHandle(JNIEnv* env, jlong handle) {
  std::lock_guard<std::mutex> lock(handleMutex);
  auto it = handles.find(handle);
  if (it == handles.end()) { fail(env, "SMB file handle is closed"); return {}; }
  return it->second;
}
}  // namespace

extern "C" {
JNIEXPORT void JNICALL
Java_remix_myplayer_data_remote_smb_SmbNativeBridge_checkConnection(
    JNIEnv* env, jobject, jstring host, jstring share, jstring user, jstring pass, jstring domain) {
  connect(env, host, share, user, pass, domain);
}

JNIEXPORT jobjectArray JNICALL
Java_remix_myplayer_data_remote_smb_SmbNativeBridge_list(
    JNIEnv* env, jobject, jstring host, jstring share, jstring user, jstring pass,
    jstring domain, jstring path) {
  auto session = connect(env, host, share, user, pass, domain);
  if (!session) return nullptr;
  const auto parent = Utf(env, path).str();
  smb2dir* dir = smb2_opendir(session->ctx, parent.c_str());
  if (!dir) {
    const auto status = static_cast<uint32_t>(smb2_get_nterror(session->ctx));
    const int rc = (status == 0xC0000034u || status == 0xC000003Au) ? -ENOENT : -EIO;
    fail(env, smb2_get_error(session->ctx) ?: "Unable to open SMB directory", rc);
    return nullptr;
  }
  struct Entry { std::string name, key; bool directory; uint64_t size, modified; };
  std::vector<Entry> items;
  while (auto* entry = smb2_readdir(session->ctx, dir)) {
    const std::string name = entry->name ? entry->name : "";
    if (name.empty() || name == "." || name == "..") continue;
    items.push_back({name, parent.empty() ? name : parent + "/" + name,
                     entry->st.smb2_type == SMB2_TYPE_DIRECTORY,
                     entry->st.smb2_size, entry->st.smb2_mtime});
  }
  smb2_closedir(session->ctx, dir);
  jclass type = env->FindClass("remix/myplayer/data/remote/RemoteFile");
  if (!type) return nullptr;
  jobjectArray result = env->NewObjectArray(static_cast<jsize>(items.size()), type, nullptr);
  for (jsize i = 0; result && i < static_cast<jsize>(items.size()); ++i) {
    const auto& entry = items[i];
    jobject file = makeFile(env, entry.name, entry.key, entry.directory, entry.size, entry.modified);
    if (!file) break;
    env->SetObjectArrayElement(result, i, file);
    env->DeleteLocalRef(file); // Critical for large directories (>512 entries).
  }
  env->DeleteLocalRef(type);
  return result;
}

JNIEXPORT jobject JNICALL
Java_remix_myplayer_data_remote_smb_SmbNativeBridge_stat(
    JNIEnv* env, jobject, jstring host, jstring share, jstring user, jstring pass,
    jstring domain, jstring path) {
  auto session = connect(env, host, share, user, pass, domain);
  if (!session) return nullptr;
  const auto key = Utf(env, path).str();
  smb2_stat_64 st{};
  int rc = smb2_stat(session->ctx, key.c_str(), &st);
  if (rc < 0) { fail(env, smb2_get_error(session->ctx) ?: "SMB stat failed", rc); return nullptr; }
  return makeFile(env, key.substr(key.find_last_of('/') == std::string::npos ? 0 : key.find_last_of('/') + 1),
                  key, st.smb2_type == SMB2_TYPE_DIRECTORY, st.smb2_size, st.smb2_mtime);
}

JNIEXPORT void JNICALL
Java_remix_myplayer_data_remote_smb_SmbNativeBridge_delete(
    JNIEnv* env, jobject, jstring host, jstring share, jstring user, jstring pass,
    jstring domain, jstring path, jboolean directory) {
  auto session = connect(env, host, share, user, pass, domain);
  if (!session) return;
  const auto key = Utf(env, path).str();
  int rc = directory ? smb2_rmdir(session->ctx, key.c_str()) : smb2_unlink(session->ctx, key.c_str());
  if (rc < 0) fail(env, smb2_get_error(session->ctx) ?: "SMB delete failed", rc);
}

JNIEXPORT jlong JNICALL
Java_remix_myplayer_data_remote_smb_SmbNativeBridge_open(
    JNIEnv* env, jobject, jstring host, jstring share, jstring user, jstring pass,
    jstring domain, jstring path) {
  auto unique = connect(env, host, share, user, pass, domain);
  if (!unique) return 0;
  const auto key = Utf(env, path).str();
  unique->file = smb2_open(unique->ctx, key.c_str(), O_RDONLY);
  if (!unique->file) {
    const auto status = static_cast<uint32_t>(smb2_get_nterror(unique->ctx));
    const int rc = (status == 0xC0000034u || status == 0xC000003Au) ? -ENOENT : -EIO;
    fail(env, smb2_get_error(unique->ctx) ?: "SMB open failed", rc);
    return 0;
  }
  smb2_stat_64 st{};
  int rc = smb2_fstat(unique->ctx, unique->file, &st);
  if (rc < 0) { fail(env, smb2_get_error(unique->ctx) ?: "SMB fstat failed", rc); return 0; }
  unique->fileSize = st.smb2_size;
  std::lock_guard<std::mutex> lock(handleMutex);
  jlong id = nextHandle++;
  handles.emplace(id, std::shared_ptr<Session>(std::move(unique)));
  return id;
}

JNIEXPORT jlong JNICALL
Java_remix_myplayer_data_remote_smb_SmbNativeBridge_size(JNIEnv* env, jobject, jlong handle) {
  auto session = getHandle(env, handle);
  return session ? static_cast<jlong>(session->fileSize) : -1;
}

JNIEXPORT jint JNICALL
Java_remix_myplayer_data_remote_smb_SmbNativeBridge_read(
    JNIEnv* env, jobject, jlong handle, jlong position, jbyteArray dest, jint offset, jint count) {
  auto session = getHandle(env, handle);
  if (!session) return -1;
  if (!dest || position < 0 || offset < 0 || count < 0 ||
      static_cast<jlong>(offset) + count > env->GetArrayLength(dest)) {
    fail(env, "Invalid SMB read arguments"); return -1;
  }
  if (count == 0) return 0;
  std::lock_guard<std::mutex> lock(session->mutex);
  if (static_cast<uint64_t>(position) >= session->fileSize) return -1;
  const size_t length = static_cast<size_t>(std::min<uint64_t>(
      {static_cast<uint64_t>(count), session->fileSize - static_cast<uint64_t>(position),
       static_cast<uint64_t>(256 * 1024), smb2_get_max_read_size(session->ctx)}));
  if (length == 0) { fail(env, "SMB negotiated a zero-byte read size"); return -1; }
  std::vector<uint8_t> buffer(length);
  int rc = smb2_pread(session->ctx, session->file, buffer.data(), static_cast<uint32_t>(length),
                      static_cast<uint64_t>(position));
  if (rc < 0) { fail(env, smb2_get_error(session->ctx) ?: "SMB read failed", rc); return -1; }
  if (rc == 0) return -1;
  env->SetByteArrayRegion(dest, offset, rc, reinterpret_cast<const jbyte*>(buffer.data()));
  return rc;
}

JNIEXPORT void JNICALL
Java_remix_myplayer_data_remote_smb_SmbNativeBridge_close(JNIEnv*, jobject, jlong handle) {
  std::shared_ptr<Session> removed;
  {
    std::lock_guard<std::mutex> lock(handleMutex);
    auto it = handles.find(handle);
    if (it != handles.end()) {
      removed = std::move(it->second);
      handles.erase(it);
    }
  }
  // Disconnect outside the global lock; in-flight readers hold their own reference.
  removed.reset();
}
}  // extern "C"
