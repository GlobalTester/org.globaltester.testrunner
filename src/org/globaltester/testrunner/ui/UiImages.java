package org.globaltester.testrunner.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
 
/**
 * This enumeration provides a number of images for the plugin.
 */
public enum UiImages {
	//TODO provide JUnit-Test for this class
	/*
	 * Based on work by Lothar Wendehals as published here:
	 * http://blogs.itemis.de/wendehal/2010/07/08/pretty-elegant-way-to-provide-images-in-eclipse-ui-plug-ins/
	 */
 
	CAMPAIGN_TESTSUITE_ICON("icons/campaign_testsuite.png"),
	CAMPAIGN_TESTCASE_ICON("icons/campaign_testcase.png"),
	CAMPAIGN_TESTSTEP_ICON("icons/campaign_teststep.png"),
	EXECUTE_ICON("icons/execute.png"),
	RESULT_ICON("icons/result.png");
 
        // add more image enumerations here...
 
 
	private final String path;
 
	private UiImages(final String path) {
		this.path = path;
	}
 
 
	/**
	 * Returns an image. Clients do not need to dispose the image, it will be disposed automatically.
	 * 
	 * @return an {@link Image}
	 */
	public Image getImage() {
		final ImageRegistry imageRegistry = Activator.getDefault().getImageRegistry();
		Image image = imageRegistry.get(this.path);
		if (image == null) {
			addImageDescriptor();
			image = imageRegistry.get(this.path);
		}
 
		return image;
	}
 
	/**
	 * Returns an image descriptor.
	 * 
	 * @return an {@link ImageDescriptor}
	 */
	public ImageDescriptor getImageDescriptor() {
		final ImageRegistry imageRegistry = Activator.getDefault().getImageRegistry();
		ImageDescriptor imageDescriptor = imageRegistry.getDescriptor(this.path);
		if (imageDescriptor == null) {
			addImageDescriptor();
			imageDescriptor = imageRegistry.getDescriptor(this.path);
		}
 
		return imageDescriptor;
	}
 
	private void addImageDescriptor() {
		final Activator activator = Activator.getDefault();
		final ImageDescriptor id = ImageDescriptor.createFromURL(activator.getBundle().getEntry(this.path));
		activator.getImageRegistry().put(this.path, id);
	}
 
}