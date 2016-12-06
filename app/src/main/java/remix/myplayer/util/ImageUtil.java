package remix.myplayer.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import remix.myplayer.application.APlayerApplication;

/**
 * Created by taeja on 16-3-23.
 */
public class ImageUtil {
    private static ImageUtil mInstance;
    public static ImageUtil getInstance(){
        return mInstance == null ? new ImageUtil() : mInstance;
    }

    /**
     * 获取图片大小
     * @param resid 图片资源id
     * @return 返回图片高度与宽度
     */
    public static int[] getImageSize(int resid){
        //获得图片参数
        BitmapFactory.Options options = new BitmapFactory.Options();
        //不为其分配内存
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(APlayerApplication.getContext().getResources(), resid,options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        String imageType = options.outMimeType;
        return imageHeight > 0 && imageWidth > 0 ? new int[]{imageHeight,imageWidth} : null;
    }

    /**
     * @param originWidth 原始高度
     * @param originHeight 原始宽度
     * @param reqWidth 期望宽度
     * @param reqHeight 期望高度
     * @return 压缩比
     */
    public static int calculateInSampleSize(int originWidth,int originHeight,int reqWidth,int reqHeight){
        int inSampleSize = 1;
        if(originHeight > reqHeight || originWidth > reqWidth){
            final int heightRatio = Math.round((float)originHeight / reqHeight);
            final int widthRatio = Math.round((float)originWidth / reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    /**
     * 将图片压缩到期望大小
     * @param reqHeight 期望高度
     * @param reqWidth 期望宽度
     * @param resid 资源id
     * @return 压缩后的bitmap
     */
    public static Bitmap decodeSampledBitmapFromResource(int reqHeight,int reqWidth,int resid){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(APlayerApplication.getContext().getResources(),resid,options);

        options.inSampleSize = calculateInSampleSize(options.outWidth,options.outHeight,reqWidth,reqHeight);
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeResource(APlayerApplication.getContext().getResources(),resid,options);
    }
}
