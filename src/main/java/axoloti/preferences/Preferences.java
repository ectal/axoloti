/**
 * Copyright (C) 2013, 2014 Johannes Taelman
 *
 * This file is part of Axoloti.
 *
 * Axoloti is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Axoloti is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Axoloti. If not, see <http://www.gnu.org/licenses/>.
 */
package axoloti.preferences;

import axoloti.Axoloti;
import axoloti.Version;
import axoloti.objectlibrary.AxoFileLibrary;
import axoloti.objectlibrary.AxoGitLibrary;
import axoloti.objectlibrary.AxolotiLibrary;
import axoloti.patch.PatchViewType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persist;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Johannes Taelman
 */
@Root
public class Preferences {

    @Attribute(required = false)
    String appVersion;

    @Element(required = false)
    String CurrentFileDirectory;

    // search path will be removed from persistance,
    // here for compatibility only
    @Deprecated
    @Element(required = false)
    String ObjectSearchPath;
    @Deprecated
    @Element(required = false)
    String ComPortName;
    @Element(required = false)
    Integer PollInterval;
    @Element(required = false)
    Boolean MouseDialAngular;
    @Element(required = false)
    Boolean MouseDoNotRecenterWhenAdjustingControls;
    @Element(required = false)
    Boolean ExpertMode;
    @ElementList(required = false)
    ArrayList<String> recentFiles = new ArrayList<>();

    @Deprecated
    @Element(required = false)
    String MidiInputDevice;
    @Element(required = false)
    String RuntimeDir;
    @Element(required = false)
    String FirmwareDir;
    @Element(required = false)
    String FavouriteDir;
    @Element(required = false)
    String ControllerObject;
    @Element(required = false)
    Boolean ControllerEnabled;
    @Element(required = false)
    String themePath;
    @Element(required = false)
    PatchViewType patchViewType;
    @Element(required = false)
    Boolean mouseWheelPanEnabled;

    @ElementMap(required = false, entry = "Boards", key = "cpuid", attribute = true, inline = true)
    HashMap<String, String> BoardNames;

    @ElementListUnion({
        @ElementList(entry = "gitlib", type = AxoGitLibrary.class, inline = true, required = false),
        @ElementList(entry = "filelib", type = AxoFileLibrary.class, inline = true, required = false)
    }
    )
    ArrayList<AxolotiLibrary> libraries;

    String[] ObjectPath;

    boolean isDirty = false;

    final int nRecentFiles = 16;

    final int minimumPollInterval = 20;

    protected Preferences() {
        if (CurrentFileDirectory == null) {
            CurrentFileDirectory = "";
        }
        ObjectSearchPath = null;

        if (PollInterval == null) {
            PollInterval = 50;
        }
        if (MouseDialAngular == null) {
            MouseDialAngular = false;
        }
        if (MouseDoNotRecenterWhenAdjustingControls == null) {
            MouseDoNotRecenterWhenAdjustingControls = false;
        }
        if (ExpertMode == null) {
            ExpertMode = false;
        }
        if (FavouriteDir == null) {
            FavouriteDir = "";
        }
        if (BoardNames == null) {
            BoardNames = new HashMap<>();
        }
        if (ControllerObject == null) {
            ControllerObject = "";
            ControllerEnabled = false;
        }

        if (libraries == null) {
            libraries = new ArrayList<>();
        }
    }

    @Persist
    public void persist() {
        // called prior to serialization
        appVersion = Version.AXOLOTI_SHORT_VERSION;
    }

    void setDirty() {
        isDirty = true;
    }

    void clearDirty() {
        isDirty = false;
    }

    public List<AxolotiLibrary> getLibraries() {
        return Collections.unmodifiableList(libraries);
    }

    public AxolotiLibrary getLibrary(String id) {
        if (libraries == null) {
            return null;
        }
        for (AxolotiLibrary lib : libraries) {
            if (lib.getId().equals(id)) {
                return lib;
            }
        }
        return null;
    }

    public String[] getObjectSearchPath() {
        return ObjectPath;
    }

    public void updateLibrary(String id, AxolotiLibrary newlib) {
        boolean found = false;
        for (AxolotiLibrary lib : libraries) {
            if (lib.getId().equals(id)) {
                if (lib != newlib) {
                    int idx = libraries.indexOf(lib);
                    libraries.set(idx, newlib);
                }
                found = true;
            }
        }
        if (!found) {
            libraries.add(newlib);
        }
        buildObjectSearchPatch();
        setDirty();
    }

    public void removeLibrary(String id) {
        for (AxolotiLibrary lib : libraries) {
            if (lib.getId().equals(id)) {
                libraries.remove(lib);
                return;
            }
        }
        setDirty();
        buildObjectSearchPatch();
    }

    public void enableLibrary(String id, boolean e) {
        for (AxolotiLibrary lib : libraries) {
            if (lib.getId().equals(id)) {
                lib.setEnabled(e);
            }
        }
        setDirty();
        buildObjectSearchPatch();
    }

    public String getCurrentFileDirectory() {
        return CurrentFileDirectory;
    }

    public int getPollInterval() {
        if (PollInterval > minimumPollInterval) {
            return PollInterval;
        }
        return minimumPollInterval;
    }

    public void setPollInterval(int i) {
        if (i < minimumPollInterval) {
            i = minimumPollInterval;
        }
        PollInterval = i;
        setDirty();
    }

    public void setCurrentFileDirectory(String CurrentFileDirectory) {
        if (this.CurrentFileDirectory.equals(CurrentFileDirectory)) {
            return;
        }
        this.CurrentFileDirectory = CurrentFileDirectory;
        savePrefs();
        setDirty();
    }

    static String getPrefsFileLoc() {
        return System.getProperty(axoloti.Axoloti.HOME_DIR) + File.separator + "axoloti.prefs";
    }

    private static Preferences singleton;

    public static Preferences getPreferences() {
        if (singleton == null) {
            File p = new File(Preferences.getPrefsFileLoc());
            if (p.exists()) {
                Preferences prefs = null;
                Serializer serializer = new Persister();
                try {
                    prefs = serializer.read(Preferences.class, p);
                } catch (Exception ex) {
                    try {
                        Logger.getLogger(Preferences.class
                                .getName()).log(Level.SEVERE, null, ex);
                        Logger.getLogger(Preferences.class
                                .getName()).log(Level.INFO, "Attempt to load preferenced in relaxed mode");
                        prefs = serializer.read(Preferences.class, p, false);
                    } catch (Exception ex1) {
                        Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
                if (prefs == null) {
                    prefs = new Preferences();
                }
                singleton = prefs;
                if (prefs.RuntimeDir
                        == null) {
                    prefs.RuntimeDir = System.getProperty(axoloti.Axoloti.RUNTIME_DIR);
                    prefs.setDirty();
                } else {
                    System.setProperty(axoloti.Axoloti.RUNTIME_DIR, prefs.RuntimeDir);
                }
                if (prefs.FirmwareDir
                        == null) {
                    prefs.FirmwareDir = System.getProperty(axoloti.Axoloti.FIRMWARE_DIR);
                    prefs.setDirty();
                } else {
                    System.setProperty(axoloti.Axoloti.FIRMWARE_DIR, prefs.FirmwareDir);
                }

                if (prefs.libraries.isEmpty()) {
                    prefs.resetLibraries(false);
                }

                prefs.buildObjectSearchPatch();

                singleton.MidiInputDevice = null; // clear it out for the future
            } else {
                singleton = new Preferences();
                singleton.resetLibraries(false);
            }
        }
        return singleton;
    }

    public void savePrefs() {
        Logger.getLogger(Preferences.class
                .getName()).log(Level.INFO, "Saving preferences...");
        Serializer serializer = new Persister();
        File f = new File(getPrefsFileLoc());

        Logger.getLogger(Preferences.class
                .getName()).log(Level.INFO, "preferences path : {0}", f.getAbsolutePath());

        try {
            serializer.write(this, f);
        } catch (Exception ex) {
            Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, null, ex);
        }

        clearDirty();
    }

    @Deprecated
    public String getComPortName() {
        return ComPortName;
    }

    @Deprecated
    public void setComPortName(String ComPortName) {
    }

    public Boolean getMouseDialAngular() {
        return MouseDialAngular;
    }

    public void setMouseDialAngular(boolean MouseDialAngular) {
        if (this.MouseDialAngular == MouseDialAngular) {
            return;
        }
        this.MouseDialAngular = MouseDialAngular;
        setDirty();
    }

    public boolean getMouseDoNotRecenterWhenAdjustingControls() {
        return MouseDoNotRecenterWhenAdjustingControls;
    }

    public void setMouseDoNotRecenterWhenAdjustingControls(boolean MouseDoNotRecenterWhenAdjustingControls) {
        if (MouseDoNotRecenterWhenAdjustingControls == this.MouseDoNotRecenterWhenAdjustingControls) {
            return;
        }
        this.MouseDoNotRecenterWhenAdjustingControls = MouseDoNotRecenterWhenAdjustingControls;
        setDirty();
    }

    public Boolean getExpertMode() {
        return ExpertMode;
    }

    public List<String> getRecentFiles() {
        return Collections.unmodifiableList(recentFiles);
    }

    public void addRecentFile(String filename) {
        for (String r : recentFiles) {
            if (r.equals(filename)) {
                return;
            }
        }
        if (recentFiles.size() == nRecentFiles) {
            recentFiles.remove(0);
        }
        recentFiles.add(filename);
        setDirty();
    }

    public String getFavouriteDir() {
        return FavouriteDir;
    }

    public void setFavouriteDir(String favouriteDir) {
        if (this.FavouriteDir.equals(favouriteDir)) {
            return;
        }
        this.FavouriteDir = favouriteDir;
        setDirty();
    }

    public void setFirmwareDir(String dir) {
        FirmwareDir = dir;
        System.setProperty(axoloti.Axoloti.FIRMWARE_DIR, dir);
    }

    public void setRuntimeDir(String dir) {
        RuntimeDir = dir;
        System.setProperty(axoloti.Axoloti.RUNTIME_DIR, dir);
    }

    public String getBoardName(String cpu) {
        if (cpu == null) {
            return null;
        }
        if (BoardNames.containsKey(cpu)) {
            return BoardNames.get(cpu);
        }
        return null;
    }

    public void setBoardName(String cpuid, String name) {
        if (name == null) {
            BoardNames.remove(cpuid);
        } else {
            BoardNames.put(cpuid, name);
        }
        setDirty();
    }

    public String getControllerObject() {
        return ControllerObject;
    }

    public void setControllerObject(String s) {
        ControllerObject = s;
    }

    public void setControllerEnabled(boolean b) {
        ControllerEnabled = b;
    }

    public boolean isControllerEnabled() {
        return ControllerEnabled;
    }

    public final void resetLibraries(boolean delete) {
        libraries = new ArrayList<>();

        try {
            AxoGitLibrary factory = new AxoGitLibrary(
                    AxolotiLibrary.FACTORY_ID,
                    "git",
                    new File(System.getProperty(axoloti.Axoloti.HOME_DIR) + File.separator + "axoloti-factory").getCanonicalPath() + File.separator,
                    true,
                    "https://github.com/axoloti/axoloti-factory.git",
                    false
            );
            libraries.add(factory);

            libraries.add(new AxoFileLibrary(
                    "home",
                    "local",
                    new File(System.getProperty(axoloti.Axoloti.HOME_DIR)).getCanonicalPath() + File.separator,
                    true
            ));

            libraries.add(new AxoGitLibrary(
                    AxolotiLibrary.USER_LIBRARY_ID,
                    "git",
                    new File(System.getProperty(axoloti.Axoloti.HOME_DIR) + File.separator + "axoloti-contrib").getCanonicalPath() + File.separator,
                    true,
                    "https://github.com/axoloti/axoloti-contrib.git",
                    false
            ));
        } catch (IOException ex) {
            Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!Axoloti.isFailSafeMode()) {
            // initialise the libraries
            for (AxolotiLibrary lib : libraries) {
                if (lib.getEnabled()) {
                    lib.init(delete);
                }
            }
        }
        buildObjectSearchPatch();
    }

    private void buildObjectSearchPatch() {
        ArrayList<String> objPath = new ArrayList<>();

        for (AxolotiLibrary lib : libraries) {
            if (lib.getEnabled()) {
                String lpath = lib.getLocalLocation() + "objects";

                //might be two libs pointing to same place
                if (!objPath.contains(lpath)) {
                    objPath.add(lpath);
                }
            }
        }
        ObjectPath = objPath.toArray(new String[0]);
    }

    public String getThemePath() {
        return themePath;
    }

    public void setThemePath(String themePath) {
        this.themePath = themePath;
        savePrefs();
    }

    public void setPatchViewType(PatchViewType patchViewType) {
        this.patchViewType = patchViewType;
    }

    public PatchViewType getPatchViewType() {
        if (patchViewType == null) {
            return PatchViewType.SWING;
        }
        return patchViewType;
    }

    public void setMouseWheelPan(Boolean mouseWheelPan) {
        this.mouseWheelPanEnabled = mouseWheelPan;
    }

    public Boolean getMouseWheelPan() {
        if (mouseWheelPanEnabled == null) {
            mouseWheelPanEnabled = false;
        }
        return mouseWheelPanEnabled;
    }
}
