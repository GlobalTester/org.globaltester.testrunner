package org.globaltester.testrunner.ui;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.globaltester.testrunner.testframework.Result.Status;
import org.osgi.framework.Bundle;
 
/**
 * This enumeration provides a number of images for the plugin.
 */
public enum NonUiImages {
	//TODO provide JUnit-Test for this class
	/*
	 * Based on work by Lothar Wendehals as published here:
	 * http://blogs.itemis.de/wendehal/2010/07/08/pretty-elegant-way-to-provide-images-in-eclipse-ui-plug-ins/
	 */
 
	STS_PASSED_ICON("icons/sts_passed.png"),
	STS_FAILED_ICON("icons/sts_failed.png"),
	STS_WARNING_ICON("icons/sts_warning.png"),
	STS_NA_ICON("icons/sts_na.png"),
	STS_NYE_ICON("icons/sts_nye.png");
 
        // add more image enumerations here...
 
 
	private final String path;
 
	private NonUiImages(final String path) {
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
		final Bundle bundle = Platform.getBundle(org.globaltester.testrunner.Activator.PLUGIN_ID);
		final ImageDescriptor id = ImageDescriptor.createFromURL(bundle.getEntry(this.path));
		activator.getImageRegistry().put(this.path, id);
	}


	public static NonUiImages valueOf(Status status) {
		switch (status) {
		case PASSED:
			return STS_PASSED_ICON;
		case FAILURE:
			return STS_FAILED_ICON;
		case WARNING:
			return STS_WARNING_ICON;
		case NOT_APPLICABLE:
			return STS_NA_ICON;
		}
		return STS_NYE_ICON;
	}
 
}