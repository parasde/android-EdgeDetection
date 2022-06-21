#include <jni.h>
#include <opencv2/opencv.hpp>

#include <android/log.h>

#define LOG_TAG "NATIVE"

#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace cv;
using namespace std;

extern "C"
JNIEXPORT void JNICALL
Java_com_parasde_example_CameraActivity_detection(JNIEnv *env, jobject instance, jlong input,jlong output, jintArray crop) {
    jint *cropRect = (*env).GetIntArrayElements(crop, NULL);

    Mat &image = *(Mat *)input;
    Mat img_gray, img_blur, img_edge;
    cvtColor(image, img_gray, COLOR_RGBA2GRAY);
    GaussianBlur(img_gray, img_blur, Size(3, 3), 0);
    Canny(img_blur, img_edge, 75, 200);

    Mat &result = *(Mat *)output;
    cvtColor(image, result, COLOR_RGBA2RGB);

    // detection 라벨링 사이즈 조절
    int maxWidth = (int)(image.cols * 0.9);
    int maxHeight = (int)(image.rows * 0.9);
    int minWidth = (int)(image.cols * 0.7);
    int minHeight = (int)(image.rows * 0.7);

    // 0 으로 초기화
    for (int i = 0; i < 4; i++) {
        cropRect[i] = 0;
    }

    Mat labels, stats, centroids;
    int numOfLabels = connectedComponentsWithStats(img_edge, labels, stats, centroids, 8, CV_32S);
    for (int j = 1; j < numOfLabels; j++) {
        int width = stats.at<int>(j, CC_STAT_WIDTH);
        int height  = stats.at<int>(j, CC_STAT_HEIGHT);

        // 라벨링 사이즈 조절
        if(width < minWidth || width > maxWidth || height < minHeight || height > maxHeight) {
            continue;
        }

        int left = stats.at<int>(j, CC_STAT_LEFT);
        int top  = stats.at<int>(j, CC_STAT_TOP);
        cropRect[0] = width;
        cropRect[1] = height;
        cropRect[2] = left;
        cropRect[3] = top;
//        (*env).ReleaseIntArrayElements(crop, cropRect, 0);
        rectangle(result, Point(left, top), Point(left + width, top + height), Scalar(255, 0, 0), 2);
        break;
    }
    (*env).ReleaseIntArrayElements(crop, cropRect, 0);
}