#include <opencv2/core/core.hpp>
//#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/highgui/highgui.hpp"
//#include "opencv2/imgproc/imgproc.hpp"
#include "opencv2/contrib/contrib.hpp"

#include <iostream>
#include <stdio.h>
#include <fstream>
#include <sstream>

using namespace std;
using namespace cv;

// Function Headers
void detectAndDisplay(Mat frame, CascadeClassifier& cascade, string outputfile);
Mat toGrayscale(InputArray _src);


// Global variables
// Cascade file used to detect the face
string faceCascadeFilename = "haarcascade_frontalface_alt.xml";

string window_name = "Capture - Face detection";

static void read_csv(const string& filename, vector<Mat>& images, vector<int>& labels, char separator = ';') {
    std::ifstream file(filename.c_str(), ifstream::in);
    if (!file) {
        string error_message = "No valid input file was given, please check the given filename.";
        CV_Error(CV_StsBadArg, error_message);
    }
    string line, path, classlabel;
    while (getline(file, line)) {
        stringstream liness(line);
        getline(liness, path, separator);
        getline(liness, classlabel);
        if(!path.empty() && !classlabel.empty()) {
            images.push_back(imread(path, 0));
            labels.push_back(atoi(classlabel.c_str()));
        }
    }
}


// Function main
int main(int argc, const char** argv )
{
    // Load the cascade
    CascadeClassifier faceDetector;
    string inputimg = argv[1];
    string outputimg = argv[2];

    try {
            faceDetector.load(faceCascadeFilename);
    } catch (cv::Exception e) {}
        if ( faceDetector.empty() ) {
            cerr << "ERROR: Couldn't load Face Detector (";
            cerr << faceCascadeFilename << ")!" << endl;
            exit(1);
    }

    // Read the image file
    Mat img = imread(inputimg , CV_LOAD_IMAGE_COLOR);
    //imshow("Original", img);

    // Apply the classifier to the frame
    if (!img.empty()){
      detectAndDisplay(img, faceDetector, outputimg);
    }
    else{
        printf(" --(!) No captured frame -- Break!");
    }

    waitKey(0);
    return 0;
}

// Function detectAndDisplay
void detectAndDisplay(Mat img, CascadeClassifier& cascade, string outputimg)
{
    Mat gray;
    Mat smallImg;
    Mat equalizedImg;
    Mat filtered;
    string text;
    stringstream sstm;

        if (img.channels() == 3) {
            cvtColor(img, gray, CV_BGR2GRAY);
        }
        else if (img.channels() == 4) {
            cvtColor(img, gray, CV_BGRA2GRAY);
        }
        else {
            // Access the grayscale input image directly.
            gray = img;
        }

        const int DETECTION_WIDTH = 320;
        // Possibly shrink the image, to run much faster.

        float scale = gray.cols / (float) DETECTION_WIDTH;
        if (gray.cols > DETECTION_WIDTH) {
            // Shrink the image while keeping the same aspect ratio.
            int scaledHeight = cvRound(gray.rows / scale);
            resize(gray, smallImg, Size(DETECTION_WIDTH, scaledHeight));
        }
        else {
            // Access the input directly since it is already small.
            smallImg = gray;
        }

        // Standardize the brightness & contrast, such as
        // to improve dark images.

        equalizeHist(smallImg, equalizedImg);

        int flags = CASCADE_FIND_BIGGEST_OBJECT |
                CASCADE_DO_ROUGH_SEARCH;// Search for only one face
        Size minFeatureSize(20, 20);     // Smallest face size.
        float searchScaleFactor = 1.1f;  // How many sizes to search.
        int minNeighbors = 4;            // Reliability vs many faces.

        // Detect objects in the small grayscale image.
        std::vector<Rect> faces;
        cascade.detectMultiScale(equalizedImg, faces, searchScaleFactor, minNeighbors, flags, minFeatureSize); // Set Region of Interest

        // Enlarge the results if the image was temporarily shrunk.
        if (gray.cols > DETECTION_WIDTH) {
            for (int i = 0; i < (int)faces.size(); i++ ) {
                faces[i].x = cvRound(faces[i].x * scale);
                faces[i].y = cvRound(faces[i].y * scale);
                faces[i].width = cvRound(faces[i].width * scale);
                faces[i].height = cvRound(faces[i].height * scale);
            }
        }

        // If the object is on a border, keep it in the image.
        for (int i = 0; i < (int)faces.size(); i++ ) {
            if (faces[i].x < 0)
                faces[i].x = 0;
            if (faces[i].y < 0)
                faces[i].y = 0;
            if (faces[i].x + faces[i].width > img.cols)
                faces[i].x = img.cols - faces[i].width;
            if (faces[i].y + faces[i].height > img.rows)
                faces[i].y = img.rows - faces[i].height;
        }

        int EYE_SX  = 0.16;
        int EYE_SY  = 0.26;
        int EYE_SW  = 0.30;
        int EYE_SH  = 0.28;

            Rect faceRect;
            Mat faceImg;

            if(faces.size() != 0){
                faceRect = faces[0];
                faceImg = gray(faceRect);
            }

            if (faces.size() != 0)
            {
                    CascadeClassifier eyeDetector1("haarcascade_eye.xml");
                    CascadeClassifier eyeDetector2("haarcascade_eye_tree_eyeglasses.xml");

                    std::vector<Rect> eyes;

                    Size minEyeFeatureSize(15, 15);
                    int flagsEye = 0 |CV_HAAR_SCALE_IMAGE ;
                      // Stores the detected eye.
                      // Search the left region using the 1st eye detector.

                      eyeDetector1.detectMultiScale(faceImg, eyes, searchScaleFactor, 6, flagsEye, minEyeFeatureSize);

                      // If it failed, search the left region using the 2nd eye
                      // detector.
                      if (eyes.size() == 0)
                          eyeDetector2.detectMultiScale(faceImg, eyes, searchScaleFactor, 6, flagsEye, minEyeFeatureSize);

                      if (faceRect.width > 0) {
                          // Draw an anti-aliased rectangle around the detected face.
                          rectangle(img, faceRect, CV_RGB(255, 255, 0), 2, CV_AA);


                          // Draw light-blue anti-aliased circles for the 2 eyes.
                       //   Scalar eyeColor = CV_RGB(0,255,255);
                       //  for(int i=0; i < eyes.size(); i++) {   // Check if the eye was detected
                       //      Point center( faceRect.x + eyes[i].x + eyes[i].width*0.5, faceRect.y + eyes[i].y + eyes[i].height*0.5 );
                       //      int radius = cvRound( (eyes[i].width + eyes[i].height)*0.25 );
                       //      circle(img, center, radius, eyeColor, 1, CV_AA);
                       //   }

                      }

                    int w = faceImg.cols;
                    int h = faceImg.rows;
                    Mat wholeFace;
                    equalizeHist(faceImg, wholeFace);
                    int midX = w/2;
                    Mat leftSide = faceImg(Rect(0,0, midX,h));
                    Mat rightSide = faceImg(Rect(midX,0, w-midX,h));
                    equalizeHist(leftSide, leftSide);
                    equalizeHist(rightSide, rightSide);

                    for (int y=0; y<h; y++) {
                        for (int x=0; x<w; x++) {
                            int v;
                            if (x < w/4) {
                                // Left 25%: just use the left face.
                                v = leftSide.at<uchar>(y,x);
                            }
                            else if (x < w*2/4) {
                                // Mid-left 25%: blend the left face & whole face.
                                int lv = leftSide.at<uchar>(y,x);
                                int wv = wholeFace.at<uchar>(y,x);
                                // Blend more of the whole face as it moves
                                // further right along the face.
                                float f = (x - w*1/4) / (float)(w/4);
                                v = cvRound((1.0f - f) * lv + (f) * wv);
                            }
                            else if (x < w*3/4) {
                                // Mid-right 25%: blend right face & whole face.
                                int rv = rightSide.at<uchar>(y,x-midX);
                                int wv = wholeFace.at<uchar>(y,x);
                                // Blend more of the right-side face as it moves
                                // further right along the face.
                                float f = (x - w*2/4) / (float)(w/4);
                                v = cvRound((1.0f - f) * wv + (f) * rv);
                            }
                            else {
                                // Right 25%: just use the right face.
                                v = rightSide.at<uchar>(y,x-midX);
                            }
                            faceImg.at<uchar>(y,x) = v;
                        }// end x loop
                    }//end y loop

                    filtered = Mat(faceImg.size(), CV_8U);
                    bilateralFilter(faceImg, filtered, 0, 20.0, 2.0);

                    resize(filtered, filtered, Size(92,112));

                    //imshow("Original with detected", img);
                    //string filename = "1.pgm";
                   // imwrite(outputimg, img);
                    //imshow("detected", filtered);

            }
            else
             {
    //          destroyWindow("detected");

             }

        if(faces.size() != 0){
           // path to your CSV
            string fn_csv = "at.csv";
            // images and corresponding labels
            vector<Mat> images;
            vector<int> labels;
            // read in the data
            try {
                read_csv(fn_csv, images, labels);
            } catch (exception&) {
                cerr << "Error opening file \"" << fn_csv << "\"." << endl;
                exit(1);
            }

            // get width and height
            int width = images[0].cols;
            int height = images[0].rows;
            // get test instances
            Mat testSample;
            filtered.copyTo(testSample);
            int testLabel = 41;
            int predictedLabel = -1;
            double confidence = 0.0;

            // ... and delete last element
            //images.pop_back();
            //labels.pop_back();
            // build the Fisherfaces model
            Ptr<FaceRecognizer> model = createFisherFaceRecognizer();
            model->train(images, labels);
            // test model
            model->predict(testSample, predictedLabel, confidence);

         //  cout << "actual class = " << testLabel << endl;
            if(confidence < 1500){
               // cout << "predicted class = " << predictedLabel << endl;
               // cout << "confidence = " << confidence << endl;
                if(predictedLabel == 41){
                        int pos_x = std::max(faces[0].tl().x - 10, 0);
                        int pos_y = std::max(faces[0].tl().y - 10, 0);

                        putText(img,"Harsh", Point(pos_x, pos_y), FONT_HERSHEY_PLAIN, 5.0, CV_RGB(255,0,0), 2.0);
                        imwrite(outputimg, img);
                }
            }
            else{
                //cout << "No Match Found" << endl;
                //cout << "confidence = " << confidence << endl;
                imwrite(outputimg, img);
            }



         }

}

Mat toGrayscale(InputArray _src) {
    Mat src = _src.getMat();
    // only allow one channel
    if(src.channels() != 1)
        CV_Error(CV_StsBadArg, "Only Matrices with one channel are supported");
    // create and return normalized image
    Mat dst;
    cv::normalize(_src, dst, 0, 255, NORM_MINMAX, CV_8UC1);
    return dst;
}
