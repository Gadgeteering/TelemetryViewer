import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

	static String versionString = "Telemetry Viewer v0.7 (2020-07-17)";
	
	@SuppressWarnings("serial")
	static JFrame window = new JFrame(versionString) {
		
		@Override public Dimension getPreferredSize() {
			
			int settingsViewWidth = SettingsView.instance.getPreferredSize().width;
			int dataStructureViewWidth = 0;
			if(!ConnectionsController.telemetryConnections.isEmpty()) {
				dataStructureViewWidth = Integer.max(new DataStructureCsvView(ConnectionsController.telemetryConnections.get(0)).getPreferredSize().width,
			                                         new DataStructureBinaryView(ConnectionsController.telemetryConnections.get(0)).getPreferredSize().width);
			}
			int configureViewWidth =     ConfigureView.instance.getPreferredSize().width;
			int settingsViewHeight =      SettingsView.instance.getPreferredSize().height;
			int controlsViewHeight = CommunicationView.instance.getPreferredSize().height;
			int controlsViewWidth  = CommunicationView.instance.getPreferredSize().width;
			
			int width = controlsViewWidth;
			if(width < dataStructureViewWidth)
				width = dataStructureViewWidth;
			if(width < settingsViewWidth + configureViewWidth)
				width = settingsViewWidth + configureViewWidth;
			int height = settingsViewHeight + controlsViewHeight + (8 * Theme.padding);

			return new Dimension(width, height);
			
		}
		
		@Override public Dimension getMinimumSize() {
			
			return getPreferredSize();
			
		}
		
	};
	static LogitechSmoothScrolling mouse = new LogitechSmoothScrolling();
	
	/**
	 * Entry point for the program.
	 * This just creates and configures the main window.
	 * 
	 * @param args    Command line arguments (not currently used.)
	 */
	@SuppressWarnings("serial")
	public static void main(String[] args) {
		
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception e){}
		
		// create the cache folder
		Path cacheDir = Paths.get("cache");
		try { Files.createDirectory(cacheDir); } catch(FileAlreadyExistsException e) {} catch(Exception e) { e.printStackTrace(); }
		
		// populate the window
		window.setLayout(new BorderLayout());
		window.add(OpenGLChartsView.instance,  BorderLayout.CENTER);
		window.add(SettingsView.instance,      BorderLayout.WEST);
		window.add(CommunicationView.instance, BorderLayout.SOUTH);
		window.add(ConfigureView.instance,     BorderLayout.EAST);
		NotificationsController.showHintUntil("Start by connecting to a device or opening a file by using the buttons below.", () -> false, true);
		
		window.setSize(window.getPreferredSize());
		window.setMinimumSize(window.getMinimumSize());
		window.setLocationRelativeTo(null);
		window.setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		// support smooth scrolling
		window.addWindowFocusListener(new WindowFocusListener() {
			@Override public void windowGainedFocus(WindowEvent we) { mouse.updateScrolling(); }
			@Override public void windowLostFocus(WindowEvent we)   { }
		});
		
		// allow the user to drag-n-drop settings/CSV/camera files
		window.setDropTarget(new DropTarget() {			
			@Override public void drop(DropTargetDropEvent event) {
				try {
					event.acceptDrop(DnDConstants.ACTION_LINK);
					@SuppressWarnings("unchecked")
					List<File> files = (List<File>) event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					String[] filepaths = new String[files.size()];
					for(int i = 0; i < files.size(); i++)
						filepaths[i] = files.get(i).getAbsolutePath();
					ConnectionsController.importFiles(filepaths);
				} catch(Exception e) {}
			}
		});
		
		// remove the caches on exit
		window.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				for(Connection connection : ConnectionsController.allConnections)
					connection.dispose();
				try { Files.deleteIfExists(cacheDir); } catch(Exception e) { }
			}
		});
		
		// show the window
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);
		
	}
	
	/**
	 * Hides the charts and settings panels, then shows the data structure screen in the middle of the main window.
	 * This method is thread-safe.
	 */
	public static void showConfigurationGui(JPanel gui) {
		
		SwingUtilities.invokeLater(() -> {
			OpenGLChartsView.instance.animator.pause();
			CommunicationView.instance.showSettings(false);
			ConfigureView.instance.close();
			window.remove(OpenGLChartsView.instance);
			window.add(gui, BorderLayout.CENTER);
			window.revalidate();
			window.repaint();
		});
		
	}
	
	/**
	 * Hides the data structure screen and shows the charts in the middle of the main window.
	 * This method is thread-safe.
	 */
	public static void hideConfigurationGui() {
		
		SwingUtilities.invokeLater(() -> {
			// do nothing if already hidden
			for(Component c : window.getContentPane().getComponents())
				if(c == OpenGLChartsView.instance)
					return;
			
			// remove the configuration GUI
			for(Component c : window.getContentPane().getComponents())
				if(c instanceof DataStructureCsvView || c instanceof DataStructureBinaryView)
					window.remove(c);
			
			window.add(OpenGLChartsView.instance, BorderLayout.CENTER);
			window.revalidate();
			window.repaint();
			OpenGLChartsView.instance.animator.resume();
		});
		
	}

}
