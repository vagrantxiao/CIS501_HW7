package cis501.parallelism.submission;

// code borrowed from https://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html

import cis501.parallelism.ChannelValues;
import cis501.parallelism.Int2D;

import java.util.concurrent.RecursiveAction;

public class ConvolutionTask extends RecursiveAction {

    final private static int RED_MASK   = 0x00ff0000;
    final private static int GREEN_MASK = 0x0000ff00;
    final private static int BLUE_MASK  = 0x000000ff;
    public static ChannelValues channelValues = new ChannelValues();

    final private float[][] kernel;
    final private Int2D srcPixels;
    final private int startRow;
    final private int numRows;
    final private Int2D dstPixels;

    ConvolutionTask(float[][] kern, Int2D src, int start, int length, Int2D dst) {
        kernel = kern;
        this.srcPixels = src;
        this.startRow = start;
        this.numRows = length;
        this.dstPixels = dst;
    }

    /** Clamp the given value to the interval [0,bound) */
    private static int clamp(int value, int bound) {
        if (value < 0) {
            return 0;
        }
        if (value < bound) {
            return value;
        }
        return bound - 1;
    }

    /** Called during parallel execution by the ForkJoinPool */
    @Override
    public void compute() {
    	//ConvolutionTask(kernel, src, 0, src.height, dst);
        // TODO: fill in code here...
    	if(this.numRows<10) {
    		convolute();
    		return;
    		
    	}
    	
    	int split = this.numRows / 2;

    	invokeAll(new ConvolutionTask(kernel, this.srcPixels, this.startRow, split, this.dstPixels),
    			  new ConvolutionTask(kernel, this.srcPixels, this.startRow+split, this.numRows-split, this.dstPixels));
    }

    /**
     * Computes a convolution based on the ctor parameters. Computes aggregated channel values for
     * all input pixels used in the blur into channelValues field.
     */
    void convolute() {
    	
        final int kernelWidthRadius = kernel[0].length >>> 1;
        final int kernelHeightRadius = kernel.length >>> 1;
        System.out.println("this.startRow:"+this.startRow);
        System.out.println("this.numRows:"+this.numRows);
        System.out.println("srcPixels.width:"+srcPixels.width);
        System.out.println("kernel.length"+kernel.length);
        for (int i = this.startRow; i < this.startRow + this.numRows; i++) {
            for (int j = 0; j < srcPixels.width; j++) {

                double newR = 0.0, newG = 0.0, newB = 0.0;

                for (int kh = 0; kh < kernel.length; kh++) {
                    for (int kw = 0; kw < kernel[0].length; kw++) {
                        int pixel = srcPixels.get(
                                clamp(j + kw - kernelWidthRadius, srcPixels.width),
                                clamp(i + kh - kernelHeightRadius, srcPixels.height));
                        // convolute each channel separately
                        final float k = kernel[kw][kh];

                        final int oldR = (pixel & RED_MASK) >> 16;
                        channelValues.red += oldR;
                        newR += oldR * k;

                        final int oldG = (pixel & GREEN_MASK) >> 8;
                        channelValues.green += oldG;
                        newG += oldG * k;

                        final int oldB = (pixel & BLUE_MASK);
                        channelValues.blue += oldB;
                        newB += oldB * k;
                    }
                }
                // Re-assemble destination pixel
                int dpixel = 0xff000000
                        | ((((int) newR) << 16) & RED_MASK)
                        | ((((int) newG) << 8) & GREEN_MASK)
                        | (((int) newB) & BLUE_MASK);
                dstPixels.set(j, i, dpixel);
            }
        }

    }

}
