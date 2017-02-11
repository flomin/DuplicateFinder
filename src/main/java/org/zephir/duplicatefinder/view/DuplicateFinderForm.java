package org.zephir.duplicatefinder.view;

import java.io.File;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.zephir.duplicatefinder.core.DuplicateFinderConstants;
import org.zephir.duplicatefinder.core.DuplicateFinderCore;
import org.zephir.util.ConsoleFormAppender;

public class DuplicateFinderForm implements DuplicateFinderConstants {
	private static Logger log = null;
	private Shell sShell = null; // @jve:decl-index=0:visual-constraint="10,10"
	private Label labelInputFolder = null;
	private Text textInputFolder = null;
	private Button buttonInputFolder = null;
	private Button buttonProceed = null;

	public static void main(final String[] args) {
		final Display display = (Display) SWTLoader.getDisplay();
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				log = Logger.getLogger(DuplicateFinderForm.class);

				DuplicateFinderForm thisClass = new DuplicateFinderForm();
				thisClass.createSShell();
				thisClass.sShell.open();

				while (!thisClass.sShell.isDisposed()) {
					if (!display.readAndDispatch()) {
						display.sleep();
					}
				}
				display.dispose();
			}
		});
	}

//	private void loadPreferences() {
//		try {
//			if (new File("PhotoRenamer.properties").exists()) {
//				Properties props = new Properties();
//				FileInputStream in = new FileInputStream("PhotoRenamer.properties");
//				props.load(in);
//				in.close();
//
//				String inputFolder = props.getProperty("inputFolder");
//				if (inputFolder != null) {
//					textInputFolder.setText(inputFolder);
//				}
//			}
//		} catch (IOException e) {
//			log.error("loadPreferences() KO: " + e, e);
//		}
//	}
//
//	private void savePreferences() {
//		try {
//			Properties props = new Properties();
//			props.setProperty("inputFolder", textInputFolder.getText());
//			FileOutputStream out = new FileOutputStream("PhotoRenamer.properties");
//			props.store(out, "---No Comment---");
//			out.close();
//
//		} catch (IOException e) {
//			log.error("savePreferences() KO: " + e, e);
//		}
//	}

	private void createSShell() {
		sShell = new Shell((Display) SWTLoader.getDisplay(), SWT.CLOSE | SWT.TITLE | SWT.MIN | SWT.MAX);
		sShell.setText("PhotoRenamer by wInd v" + VERSION);
		sShell.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/skull2-16x16.gif")));
		sShell.setLayout(null);
		sShell.addShellListener(new org.eclipse.swt.events.ShellAdapter() {
			@Override
			public void shellClosed(final org.eclipse.swt.events.ShellEvent e) {
//				savePreferences();
				ConsoleFormAppender.closeAll();
			}
		});

		int y = FORM_LINE_SPACE;
		labelInputFolder = new Label(sShell, SWT.HORIZONTAL);
		labelInputFolder.setText("Input folder :");
		labelInputFolder.setBounds(new Rectangle(3, y + FORM_LABEL_DELTA, FORM_LINE_TAB, FORM_LINE_HEIGHT));
		textInputFolder = new Text(sShell, SWT.BORDER);
		textInputFolder.setText(USER_DIR);
		textInputFolder.setBounds(new Rectangle(FORM_LINE_TAB + FORM_LINE_SPACE, y, 450, FORM_LINE_HEIGHT));
		textInputFolder.setTextLimit(655);
		buttonInputFolder = new Button(sShell, SWT.NONE);
		buttonInputFolder.setText("...");
		buttonInputFolder.setBounds(new Rectangle(560, y, 29, FORM_LINE_HEIGHT));
		buttonInputFolder.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				DirectoryDialog directoryDialog = new DirectoryDialog(sShell);
				directoryDialog.setFilterPath(textInputFolder.getText());
				String dir = directoryDialog.open();
				if (dir != null) {
					textInputFolder.setText(dir);
				}
			}
		});

		y += FORM_LINE_HEIGHT + FORM_LINE_SPACE;
		buttonProceed = new Button(sShell, SWT.NONE);
		buttonProceed.setText("Proceed !");
		buttonProceed.setBounds(new Rectangle(386, y, 119, FORM_BUTTON_HEIGHT));
		buttonProceed.addMouseListener(new org.eclipse.swt.events.MouseAdapter() {
			@Override
			public void mouseDown(final org.eclipse.swt.events.MouseEvent e) {
				proceed();
			}
		});

		y += (FORM_LINE_HEIGHT + FORM_LINE_SPACE) * 2;
		sShell.setSize(new Point(600, 100));
//		loadPreferences();
	}

//	private void openHelpDialog(final String title, final String text) {
//		MessageBox mb = new MessageBox(sShell, SWT.OK | SWT.ICON_QUESTION);
//		mb.setText(title);
//		mb.setMessage(text);
//		mb.open();
//	}

	private void proceed() {
		try {
			buttonProceed.setEnabled(false);

			final DuplicateFinderCore core = new DuplicateFinderCore();
			core.setFolderToProcess(new File(textInputFolder.getText()));

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						// show console
						ConsoleFormAppender.focus();

						// process
						core.processFolderWithLIRE();

					} catch (Exception e) {
						e.printStackTrace();
						log.debug("Exception: " + e.toString());
						
					} finally {
						// processing finished
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								buttonProceed.setEnabled(true);
							}
						});
					}
				}
			};
			new Thread(runnable).start();
		} catch (Exception e) {
			log.error("Error: " + e, e);
			buttonProceed.setEnabled(true);
			return;
		}
	}
}
