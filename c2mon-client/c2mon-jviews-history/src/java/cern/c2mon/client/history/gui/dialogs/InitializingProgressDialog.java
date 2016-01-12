/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 * 
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 * 
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
package cern.c2mon.client.history.gui.dialogs;

import java.awt.Component;

import javax.swing.JOptionPane;

import cern.c2mon.client.ext.history.C2monHistoryGateway;
import cern.c2mon.client.ext.history.common.event.HistoryPlayerAdapter;
import cern.c2mon.client.ext.history.common.event.HistoryPlayerListener;
import cern.c2mon.client.ext.history.common.event.HistoryProviderAdapter;
import cern.c2mon.client.ext.history.common.event.HistoryProviderListener;
import cern.c2mon.client.history.gui.dialogs.generic.ProgressDialog;
import cern.c2mon.client.history.gui.dialogs.generic.ProgressDialogActionListener;

/**
 * The {@link #getHistoryPlayerEvents()} should be hooked up to receive events
 * from {@link HistoryPlayerListener} and the
 * {@link #getHistoryProviderEvents()} should be hooked up to recieve events
 * from {@link HistoryProviderListener}. It will then appear when the history is
 * initializing, and will disappear when it is finish initializing.
 * 
 * @author vdeila
 * 
 */
public class InitializingProgressDialog {

  /** The title of the progress dialog when loading initial historical data */
  private static final String PROGRESS_TITLE = "Initializing historical data";

  /**
   * The progress text in the progress dialog when loading initial historical
   * data
   */
  private static final String PROGRESS_TEXT = "Please wait while History Player is initializing new data";

  /** Dialogs will use this as the parent component */
  private final Component parentComponent;

  /** The progress dialog which appear when initializing new data tags */
  private ProgressDialog initializingProgressDialog;

  /** Event listener for the history player */
  private final HistoryPlayerListener historyPlayerEvents;

  /** Event listener for the history provider */
  private final HistoryProviderListener historyProviderEvents;

  /**
   * 
   * @param parentComponent
   *          The parent of the dialog
   * 
   * @see #getHistoryPlayerEvents()
   * @see #getHistoryProviderEvents()
   */
  public InitializingProgressDialog(final Component parentComponent) {
    this.parentComponent = parentComponent;
    this.initializingProgressDialog = null;
    this.historyPlayerEvents = new HistoryPlayerEvents();
    this.historyProviderEvents = new HistoryProviderEvents();
  }

  /**
   * 
   * @return The initializingProgressDialog, creates it if it doesn't exists.
   */
  private ProgressDialog getInitializingProgressDialog() {
    if (this.initializingProgressDialog == null) {
      this.initializingProgressDialog = new ProgressDialog(PROGRESS_TITLE, PROGRESS_TEXT);
      this.initializingProgressDialog.setEnableCancelButton(true);
      this.initializingProgressDialog.setCancelDelegate(new ProgressDialogActionListener() {
        @Override
        public void onCancel(final ProgressDialog progressDialog) {
          progressDialog.setMessage("Stopping history playback, please wait..");
          progressDialog.setStatus("");
          C2monHistoryGateway.getHistoryManager().stopHistoryPlayerMode();
        }
      });
    }
    return this.initializingProgressDialog;
  }

  /** Events from the HistoryPlayer */
  class HistoryPlayerEvents extends HistoryPlayerAdapter {
    @Override
    public void onStoppedLoadingDueToOutOfMemory() {
      JOptionPane.showMessageDialog(parentComponent, "TIM Viewer is almost out of memory, the loading of the history have therefore stopped.\n"
          + "Please close some views or choose a shorter time period if you want to load more of the history.\n"
          + "Restart the history mode to begin loading more.", "History player - Out Of Memory!", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void onInitializingHistoryStarted() {
      getInitializingProgressDialog().setProgress(null);
      getInitializingProgressDialog().setStatus(null);
      getInitializingProgressDialog().setParent(parentComponent);

      // Shows the progress bar
      getInitializingProgressDialog().show();
    }

    @Override
    public void onInitializingHistoryProgressStatusChanged(final String progressMessage) {
      getInitializingProgressDialog().setStatus(progressMessage);
    }

    @Override
    public void onInitializingHistoryFinished() {
      // Hides the progress bar
      getInitializingProgressDialog().hide();
    }
  }

  /**
   * Events of the HistoryProvider
   */
  class HistoryProviderEvents extends HistoryProviderAdapter {

    @Override
    public void queryProgressChanged(final double percent) {
      getInitializingProgressDialog().setProgress(percent);
    }

  }

  /**
   * @return the historyPlayerEvents
   */
  public HistoryPlayerListener getHistoryPlayerEvents() {
    return historyPlayerEvents;
  }

  /**
   * @return the historyProviderEvents
   */
  public HistoryProviderListener getHistoryProviderEvents() {
    return historyProviderEvents;
  }

}
