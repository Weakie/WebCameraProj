#include <iostream>

#include "com_weakie_encoder_mpeg_jni_MPEGEncoder.h"
#include "com_weakie_encoder.h"

using namespace std;

/*
 * Class:     com_weakie_encoder_mpeg_jni_MPEGEncoder
 * Method:    register
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_weakie_encoder_mpeg_jni_MPEGEncoder_register
(JNIEnv *env, jclass cls){
	cout<<"FFMPEF Register"<<endl;
	/* register all the codecs */
	avcodec_register_all();
}

/*
 * Class:     com_weakie_encoder_mpeg_jni_MPEGEncoder
 * Method:    init
 * Signature: (II)V
 */
JNIEXPORT jlong JNICALL Java_com_weakie_encoder_mpeg_jni_MPEGEncoder_init
(JNIEnv *env, jclass cls, jint width, jint height, jint frameSize, jint srcFmt){
	cout<<"Initialize Encoder Metedata"<<endl;
	//create the new encoder Object
	Encoder* encodeObj = new Encoder(width,height,frameSize,(AVPixelFormat)srcFmt,AV_CODEC_ID_MPEG1VIDEO);
	if(encodeObj->init()==-1){
		//if init fail,delete the object and return -1;
		delete encodeObj;
		return -1;
	}
	//if success, return the ptr value as a long value
	long ptr = (long)encodeObj;
	return ptr;
}

/*
 * Class:     com_weakie_encoder_mpeg_jni_MPEGEncoder
 * Method:    encode
 * Signature: (Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)V
 */
JNIEXPORT jint JNICALL Java_com_weakie_encoder_mpeg_jni_MPEGEncoder_encode
(JNIEnv *env, jclass cls, jlong ptr, jobject in, jobject out){
	//check the ptr value
	if(ptr == -1){
		cout<<"ptr value is invalid: "<<ptr<<endl;
		return -5;
	}
	//get the buffer address and capacity
	jbyte * bbuf_in;
	jbyte * bbuf_out;
	bbuf_in = (jbyte *)env->GetDirectBufferAddress(in);
	jlong cap_in = env->GetDirectBufferCapacity(in);
	bbuf_out= (jbyte *)env->GetDirectBufferAddress(out);
	jlong cap_out = env->GetDirectBufferCapacity(out);
	//initialize the ptr and encode data
	Encoder* encoderObj = (Encoder*)ptr;
	int size = encoderObj->encodeData(bbuf_in,cap_in,bbuf_out,cap_out);
	return size;
}

/*
 * Class:     com_weakie_encoder_mpeg_jni_MPEGEncoder
 * Method:    destroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_weakie_encoder_mpeg_jni_MPEGEncoder_destroy
(JNIEnv *env, jclass cls, jlong ptr){
	cout<<"Delete The Encoder Object."<<endl;
	//check the ptr value
	if(ptr == -1){
		cout<<"ptr value is invalid: "<<ptr<<endl;
		return;
	}
	//delete the encoder object
	Encoder* encoderObj = (Encoder*)ptr;
	delete encoderObj;
}

