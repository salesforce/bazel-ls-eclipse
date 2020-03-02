package com.salesforce.b2eclipse.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class B2EPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "bazel-eclipse"; //$NON-NLS-1$

	// The shared instance
	private static B2EPlugin plugin;
	
	public B2EPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static B2EPlugin getDefault() {
		return plugin;
	}

	/**
	 * Utility method to log errors.
	 *
	 * @param thr
	 *            The exception through which we noticed the error
	 */
	public static void logError(final Throwable thr) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, 0, thr.getMessage(), thr));
	}

	/**
	 * Utility method to log errors.
	 *
	 * @param message
	 *            User comprehensible message
	 * @param thr
	 *            The exception through which we noticed the error
	 */
	public static void logError(final String message, final Throwable thr) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, 0, message, thr));
	}

	/**
	 * Log an info message for this plug-in
	 *
	 * @param message
	 */
	public static void logInfo(final String message) {
		getDefault().getLog().log(new Status(IStatus.INFO, PLUGIN_ID, 0, message, null));
	}

	/**
	 * Utility method to log warnings for this plug-in.
	 *
	 * @param message
	 *            User comprehensible message
	 * @param thr
	 *            The exception through which we noticed the warning
	 */
	public static void logWarning(final String message, final Throwable thr) {
		getDefault().getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, 0, message, thr));
	}
}
