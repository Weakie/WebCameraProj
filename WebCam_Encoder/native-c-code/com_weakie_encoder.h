#ifndef ENCODE_WEAKIE
#define ENCODE_WEAKIE
/*
 * Copyright (c) 2001 Fabrice Bellard
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Note that libavcodec only handles codecs (mpeg, mpeg4, etc...),
 * not file formats (avi, vob, mp4, mov, mkv, mxf, flv, mpegts, mpegps, etc...). See library 'libavformat' for the
 * format handling
 */
extern "C" {

#ifndef __STDC_CONSTANT_MACROS
#define __STDC_CONSTANT_MACROS
#endif

#include <iostream>
using namespace std;

#if _MSC_VER
#define snprintf _snprintf
#endif

#include <libavutil/opt.h>
#include <libavcodec/avcodec.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
}

#pragma comment(lib, "avutil.lib")  //ffmpeg
#pragma comment(lib, "avcodec.lib")
#pragma comment(lib, "swscale.lib")
class Encoder{
private:
	//
	AVCodec *codec;
	AVCodecContext *c;
	int ret, got_output;
	FILE *f;
	AVFrame *frame;
	AVPacket pkt;
	struct AVRational rational;
	//convert frame
	AVFrame *frameRGB;
	struct SwsContext* swsContext;
	//init parameter
	int width,height,frameSize;
	enum AVCodecID codec_id;
	enum AVPixelFormat srcFmt;
	int framePts;
public:
	Encoder(int width,int height,int frameSize,enum AVPixelFormat srcFmt,enum AVCodecID codec_id)
		:width(width),height(height),frameSize(frameSize),srcFmt(srcFmt),codec_id(codec_id){
		framePts = 0;
		rational.num = 1;
		rational.den = 25;
		c = NULL;
		swsContext = NULL;
	}

	/*
	 * return : -1 fail, 1 success
	 */
	int init(){
		/* find the mpeg1 video encoder */
		codec = avcodec_find_encoder(codec_id);
		if (!codec) {
			cout<<"Codec not found"<<endl;;
			return -1;
		}

		c = avcodec_alloc_context3(codec);
		if (!c) {
			cout<<"Could not allocate video codec context"<<endl;
			return -1;
		}

		/* put sample parameters */
		c->bit_rate = 800000;
		/* resolution must be a multiple of two */
		c->width = this->width;
		c->height = this->height;
		/* frames per second */
		c->time_base = rational;//(AVRational){1,25};
		/* emit one intra frame every ten frames
		* check frame pict_type before passing frame
		* to encoder, if frame->pict_type is AV_PICTURE_TYPE_I
		* then gop_size is ignored and the output of encoder
		* will always be I frame irrespective to gop_size
		*/
		c->gop_size = 10;
		c->max_b_frames = 1;
		c->pix_fmt = AV_PIX_FMT_YUV420P;
		if (codec_id == AV_CODEC_ID_H264)
			av_opt_set(c->priv_data, "preset", "slow", 0);

		/* open it */
		if (avcodec_open2(c, codec, NULL) < 0) {
			cout<<"Could not open codec"<<endl;
			return -1;
		}
		frame = av_frame_alloc();
		if (!frame) {
			cout<<"Could not allocate video frame"<<endl;
			return -1;
		}
		frame->format = c->pix_fmt;
		frame->width  = c->width;
		frame->height = c->height;

		/* the image can be allocated by any means and av_image_alloc() is
		* just the most convenient way if av_malloc() is to be used */
		ret = av_image_alloc(frame->data, frame->linesize, c->width, c->height,
									 c->pix_fmt, 32);
		if (ret < 0) {
			cout<<"Could not allocate raw picture buffer"<<endl;
			return -1;
		}

		//init rgb frame
		frameRGB = av_frame_alloc();
		if (!frameRGB) {
			cout<<"Could not allocate video frame"<<endl;
			return -1;
		}
		frameRGB->format = srcFmt;
		frameRGB->width  = c->width;
		frameRGB->height = c->height;

		/* the image can be allocated by any means and av_image_alloc() is
		* just the most convenient way if av_malloc() is to be used */
		ret = av_image_alloc(frameRGB->data, frameRGB->linesize, c->width, c->height,
									 srcFmt, 32);
		if (ret < 0) {
			cout<<"Could not allocate raw picture buffer"<<endl;
			return -1;
		}
		//init swsContext
		this->swsContext = sws_getContext(c->width,c->height,srcFmt,
				c->width,c->height,AV_PIX_FMT_YUV420P,SWS_BICUBIC,NULL,NULL,NULL);
		return 1;
	}

	~Encoder(){
		avcodec_close(c);
		av_free(c);
		av_freep(&frame->data[0]);
		av_frame_free(&frame);
		av_freep(&frameRGB->data[0]);
		av_frame_free(&frameRGB);
		sws_freeContext(swsContext);
	}

	/*
	 * return: the size of data output, -1 means fail.
	 */
	int encodeData(jbyte* in,int inSizeLimit,jbyte* out,int outSizeLimit){
		av_init_packet(&pkt);
		pkt.data = NULL;    // packet data will be allocated by the encoder
		pkt.size = 0;

		fflush(stdout);
		//copy buffer data to frame
		if(inSizeLimit<frameSize){
			cout<<"Error invalid encode in size " <<inSizeLimit<< " out size " <<outSizeLimit<<endl;
			return -1;
		}
		memcpy(frameRGB->data[0],in,frameSize);
		sws_scale(this->swsContext,frameRGB->data,frameRGB->linesize,0,c->height,frame->data,frame->linesize);
		frame->pts = framePts++;

		/* encode the image */
		ret = avcodec_encode_video2(c, &pkt, frame, &got_output);
		if (ret < 0) {
			cout<< "Error encoding frame"<<endl;
			return -2;
		}

		if (got_output) {
			cout<<"Write frame "<<framePts<<" (size="<<pkt.size<<")"<<endl;
			if(pkt.size>outSizeLimit){
				return -3;
			}
			memcpy(out, pkt.data, pkt.size);
			int retSize = pkt.size;
			av_free_packet(&pkt);
			return retSize;
		}

		return -4;
	}
};

#endif