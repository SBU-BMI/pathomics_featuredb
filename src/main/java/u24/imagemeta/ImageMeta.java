package u24.imagemeta;

import loci.common.DebugTools;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;

public class ImageMeta {

	private IFormatReader imgReader;
	
	public ImageMeta(String imageFile) { 
    		DebugTools.enableLogging("OFF");
		imgReader = new ImageReader();
		try {
			imgReader.setId(imageFile);	
		} catch (Exception e) {
			System.err.println("Stack trace: " + e.getMessage());
		}
	}	

	public int getWidth() {
		return imgReader.getSizeX();
	}

	public int getHeight() {
		return imgReader.getSizeY();
	}


	public static void main(String[] args) {
		
		String fileName = args[0];

		System.out.println("filename: " + fileName);
		ImageMeta imgMeta = new ImageMeta(fileName);

		System.out.println("Width: " + imgMeta.getWidth() + " Height: " + imgMeta.getHeight());	
	}

}
