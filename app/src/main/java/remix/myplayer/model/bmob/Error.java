package remix.myplayer.model.bmob;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/5/8 14:59
 */

public class Error extends Feedback {
    public String Title;
    public String Description;

    public Error(){}

    public Error(String title,String description, String appVersion, String appVersionCode,String display, String cpuABI, String deviceManufacturer, String deviceModel, String releaseVersion, String sdkVersion) {
        super();
        Title = title;
        Description = description;
        AppVersion = appVersion;
        AppVersionCode = appVersionCode;
        Display = display;
        CpuABI = cpuABI;
        DeviceManufacturer = deviceManufacturer;
        DeviceModel = deviceModel;
        ReleaseVersion = releaseVersion;
        SdkVersion = sdkVersion;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }
}
