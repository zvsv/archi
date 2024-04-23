/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package com.archimatetool.editor.preferences;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.editor.ArchiPlugin;
import com.archimatetool.editor.ui.ArchiLabelProvider;
import com.archimatetool.editor.ui.ColorFactory;
import com.archimatetool.editor.ui.IArchiImages;
import com.archimatetool.editor.ui.ImageFactory;
import com.archimatetool.editor.ui.ThemeUtils;
import com.archimatetool.editor.ui.components.CustomColorDialog;
import com.archimatetool.editor.ui.factory.model.FolderUIProvider;
import com.archimatetool.editor.utils.PlatformUtils;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.util.ArchimateModelUtils;


/**
 * Colours Preferences Page
 * 
 * @author Phillip Beauvoir
 */
public class ColoursPreferencePage
extends PreferencePage
implements IWorkbenchPreferencePage, IPreferenceConstants {
    
    public static String ID = "com.archimatetool.editor.prefsColours"; //$NON-NLS-1$
    public static String HELPID = "com.archimatetool.help.prefsAppearance"; //$NON-NLS-1$
    
    // Cache of objects' colours
    private Hashtable<Object, Color> fColorsCache = new Hashtable<Object, Color>();
    
    // Image Registry for Tree colors
    private ImageRegistry fImageRegistry;
    
    // Buttons
    private Button fPersistUserDefaultColors;
    private Button fEditFillColorButton;
    private Button fResetFillColorButton;
    private Button fDeriveElementLineColorsButton;
    
    // Tree
    private TreeViewer fTreeViewer;

    private IPropertyChangeListener themeChangeListener;
    
    private static List<String> themeColors = List.of(VIEW_BACKGROUND_COLOR,
                                                      VISUALISER_BACKGROUND_COLOR);
    
    // Convenience model class for Tree
    private static class TreeGrouping {
        public String title;
        public Object[] children;
 
        public TreeGrouping(String title, Object[] children) {
            this.title = title;
            this.children = children;
        }
    }
    
	public ColoursPreferencePage() {
		setPreferenceStore(ArchiPlugin.PREFERENCES);
	}
	
    @Override
    protected Control createContents(Composite parent) {
        // Help
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, HELPID);
        
        // Reset everything
        resetColorsCache(false);
        fImageRegistry = new ImageRegistry();
        
        Composite client = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = layout.marginHeight = 0;
        client.setLayout(layout);
        
        Label label = new Label(client, SWT.NULL);
        label.setText(Messages.ColoursPreferencePage_0);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);
        
        // Tree
        fTreeViewer = new TreeViewer(client);
        GridDataFactory.create(GridData.FILL_BOTH).hint(SWT.DEFAULT, 200).applyTo(fTreeViewer.getTree());
        
        // Tree Double-click listener
        fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                Object[] selected = ((IStructuredSelection)fTreeViewer.getSelection()).toArray();
                if(isValidTreeSelection(selected)) {
                    RGB newRGB = openColorDialog(selected[0]);
                    if(newRGB != null) {
                        for(Object object : selected) {
                            setColor(object, newRGB);
                        }
                    }
                }
            }
        });
        
        // Tree Selection Changed Listener
        fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() { 
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                Object[] selected = ((IStructuredSelection)event.getSelection()).toArray();
                fEditFillColorButton.setEnabled(isValidTreeSelection(selected));
                fResetFillColorButton.setEnabled(isValidTreeSelection(selected));
            }
        });
        
        // Tree Content Provider
        fTreeViewer.setContentProvider(new ITreeContentProvider() {

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            @Override
            public Object[] getElements(Object inputElement) {
                if(inputElement instanceof String) {
                    return new Object[] {
                            new TreeGrouping(Messages.ColoursPreferencePage_1, ArchimateModelUtils.getStrategyClasses()),
                            new TreeGrouping(Messages.ColoursPreferencePage_2, ArchimateModelUtils.getBusinessClasses()),
                            new TreeGrouping(Messages.ColoursPreferencePage_3, ArchimateModelUtils.getApplicationClasses()),
                            new TreeGrouping(Messages.ColoursPreferencePage_4, ArchimateModelUtils.getTechnologyClasses()),
                            new TreeGrouping(Messages.ColoursPreferencePage_5, ArchimateModelUtils.getPhysicalClasses()),
                            new TreeGrouping(Messages.ColoursPreferencePage_6, ArchimateModelUtils.getMotivationClasses()),
                            new TreeGrouping(Messages.ColoursPreferencePage_7, ArchimateModelUtils.getImplementationMigrationClasses()),
                            new TreeGrouping(Messages.ColoursPreferencePage_8, ArchimateModelUtils.getOtherClasses() ),
                            
                            new TreeGrouping(Messages.ColoursPreferencePage_9,
                                    new Object[] { IArchimatePackage.eINSTANCE.getDiagramModelNote(),
                                                   IArchimatePackage.eINSTANCE.getDiagramModelGroup() } ),
                            
                            new TreeGrouping(Messages.ColoursPreferencePage_10, new FolderType[] {
                                    FolderType.STRATEGY,
                                    FolderType.BUSINESS,
                                    FolderType.APPLICATION,
                                    FolderType.TECHNOLOGY,
                                    FolderType.MOTIVATION,
                                    FolderType.IMPLEMENTATION_MIGRATION,
                                    FolderType.OTHER,
                                    FolderType.RELATIONS,
                                    FolderType.DIAGRAMS }),
                            
                            new TreeGrouping(Messages.ColoursPreferencePage_25, themeColors.toArray()),
                            
                            DEFAULT_ELEMENT_LINE_COLOR,
                            DEFAULT_CONNECTION_LINE_COLOR
                    };
                }
                
                return null;
            }

            @Override
            public Object[] getChildren(Object parentElement) {
                if(parentElement instanceof TreeGrouping) {
                    return ((TreeGrouping)parentElement).children;
                }
                
                return null;
            }

            @Override
            public Object getParent(Object element) {
                return null;
            }

            @Override
            public boolean hasChildren(Object element) {
                return element instanceof TreeGrouping;
            }
            
        });
        
        // Tree Label Provider
        fTreeViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                if(element instanceof EClass) {
                    return ArchiLabelProvider.INSTANCE.getDefaultName((EClass)element);
                }
                if(element instanceof TreeGrouping) {
                    return ((TreeGrouping)element).title;
                }
                if(element instanceof String) {
                    String s = (String)element;
                    
                    // Theme colors
                    if(themeColors.contains(s)) {
                        return ThemeUtils.getColorDefinitionName(s);
                    }
                    
                    switch(s) {
                        case DEFAULT_ELEMENT_LINE_COLOR:
                            return Messages.ColoursPreferencePage_11;
                        case DEFAULT_CONNECTION_LINE_COLOR:
                            return Messages.ColoursPreferencePage_12;
                        default:
                            return s;
                    }
                }
                if(element instanceof FolderType) {
                    return ((FolderType)element).getLabel();
                }
                
                return null;
            }
            
            @Override
            public Image getImage(Object element) {
                if(element instanceof TreeGrouping) {
                    return IArchiImages.ImageFactory.getImage(IArchiImages.ICON_FOLDER_DEFAULT);
                }

                return getColorSwatch(element);
            }
            
            // Create a coloured image based on colour and add to the image registry
            private Image getColorSwatch(Object object) {
                String key = getColorKey(object);
                Image image = fImageRegistry.get(key);
                if(image == null) {
                    image = new Image(Display.getCurrent(), 16, 16);
                    GC gc = new GC(image);
                    if(object != null && fColorsCache.get(object) != null) {
                        gc.setBackground(fColorsCache.get(object));
                    }
                    gc.fillRectangle(0, 0, 15, 15);
                    gc.drawRectangle(0, 0, 15, 15);
                    gc.dispose();
                    image = ImageFactory.getAutoScaledImage(image);
                    fImageRegistry.put(key, image);
                }
                
                return image;
            }
            
        });
        
        //fTreeViewer.setAutoExpandLevel(2);

        // Buttons
        Composite buttonClient = new Composite(client, SWT.NULL);
        gd = new GridData(SWT.TOP, SWT.TOP, false, false);
        buttonClient.setLayoutData(gd);
        buttonClient.setLayout(new GridLayout());
        
        // Edit...
        fEditFillColorButton = new Button(buttonClient, SWT.PUSH);
        fEditFillColorButton.setText(Messages.ColoursPreferencePage_13);
        setButtonLayoutData(fEditFillColorButton);
        fEditFillColorButton.setEnabled(false);
        fEditFillColorButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Object[] selected = ((IStructuredSelection)fTreeViewer.getSelection()).toArray();
                if(isValidTreeSelection(selected)) {
                    RGB newRGB = openColorDialog(selected[0]);
                    if(newRGB != null) {
                        for(Object object : selected) {
                            setColor(object, newRGB);
                        }
                    }
                }
            }
        });

        // Reset
        fResetFillColorButton = new Button(buttonClient, SWT.PUSH);
        fResetFillColorButton.setText(Messages.ColoursPreferencePage_14);
        setButtonLayoutData(fResetFillColorButton);
        fResetFillColorButton.setEnabled(false);
        fResetFillColorButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Object[] selected = ((IStructuredSelection)fTreeViewer.getSelection()).toArray();
                if(isValidTreeSelection(selected)) {
                    for(Object object : selected) {
                        resetColorToInbuiltDefault(object);
                    }
                }
            }
        });
        
        // Import Scheme
        Button importButton = new Button(buttonClient, SWT.PUSH);
        importButton.setText(Messages.ColoursPreferencePage_15);
        setButtonLayoutData(importButton);
        importButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        importButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    importUserColors();
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        // Export Scheme
        Button exportButton = new Button(buttonClient, SWT.PUSH);
        exportButton.setText(Messages.ColoursPreferencePage_16);
        setButtonLayoutData(exportButton);
        exportButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    exportUserColors();
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        // Derive element line colours
        fDeriveElementLineColorsButton = new Button(client, SWT.CHECK);
        fDeriveElementLineColorsButton.setText(Messages.ColoursPreferencePage_18);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        fDeriveElementLineColorsButton.setLayoutData(gd);
        fDeriveElementLineColorsButton.setSelection(getPreferenceStore().getBoolean(DERIVE_ELEMENT_LINE_COLOR));
        
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;

        // Persist user default colours
        fPersistUserDefaultColors = new Button(client, SWT.CHECK);
        fPersistUserDefaultColors.setText(Messages.ColoursPreferencePage_21);
        fPersistUserDefaultColors.setLayoutData(gd);
        fPersistUserDefaultColors.setSelection(getPreferenceStore().getBoolean(SAVE_USER_DEFAULT_COLOR));
        
        // Set tree input
        fTreeViewer.setInput(""); //$NON-NLS-1$
        
        return client;
    }
    
    /**
     * @param object
     * @return The string key associated with a object
     */
    private String getColorKey(Object object) {
        if(object instanceof String) {
            return (String)object;
        }
        else if(object instanceof EClass) {
            return ((EClass)object).getName();
        }
        else if(object instanceof FolderType) {
            return ((FolderType)object).getName();
        }
        return "x"; //$NON-NLS-1$
    }
    
    /**
     * @param object Selected object
     * @return the RGB value or null
     * Open the color dialog to edit color for an object
     */
    private RGB openColorDialog(Object object) {
        CustomColorDialog colorDialog = new CustomColorDialog(getShell());
        colorDialog.setRGB(fColorsCache.get(object).getRGB());
        return colorDialog.open();
    }
    
    /**
     * @param selected
     * @return true if selected tree objects are valid
     */
    private boolean isValidTreeSelection(Object[] selected) {
        if(selected == null || selected.length == 0) {
            return false;
        }
        
        for(Object o : selected) {
            if(o instanceof TreeGrouping) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Reset the cached colour to the inbuilt default
     * @param eClass
     */
    private void resetColorToInbuiltDefault(Object object) {
        RGB defaultRGB = null;

        // Element line color - use any ArchiMate eClass as there is only one line color pref
        if(object.equals(DEFAULT_ELEMENT_LINE_COLOR)) {
            defaultRGB = ColorFactory.getInbuiltDefaultLineColor(IArchimatePackage.eINSTANCE.getBusinessActor()).getRGB();
        }
        // Connection line color - use this eClass as there is only one line color pref
        else if(object.equals(DEFAULT_CONNECTION_LINE_COLOR)) {
            defaultRGB = ColorFactory.getInbuiltDefaultLineColor(IArchimatePackage.eINSTANCE.getDiagramModelConnection()).getRGB();
        }
        // Fill color
        else if(object instanceof EClass) {
            EClass eClass = (EClass)object;
            defaultRGB = ColorFactory.getInbuiltDefaultFillColor(eClass).getRGB();
        }
        
        // Folder
        else if(object instanceof FolderType) {
            defaultRGB = FolderUIProvider.getDefaultFolderColor((FolderType)object).getRGB();
        }
        
        // Theme color
        else if(object instanceof String && themeColors.contains(object)) {
            defaultRGB = ThemeUtils.getDefaultThemeColor((String)object);
        }
        
        setColor(object, defaultRGB);
    }
    
    /**
     * @param object
     * @param rgb
     * Set a cached color for an object
     */
    private void setColor(Object object, RGB rgb) {
        if(object == null || rgb == null) {
            return;
        }
        
        fColorsCache.put(object, new Color(rgb));
        fImageRegistry.remove(getColorKey(object)); // remove from image registry so we can generate a new image
        fTreeViewer.update(object, null);
    }
    
    /**
     * @param useInbuiltDefaults if true reset to inbuilt defaults
     * Reset the color cache to user or inbuilt defaults
     */
    private void resetColorsCache(boolean useInbuiltDefaults) {
        fColorsCache.clear();
        
        for(EClass eClass : ArchimateModelUtils.getAllArchimateClasses()) {
            Color color = useInbuiltDefaults ? ColorFactory.getInbuiltDefaultFillColor(eClass) : ColorFactory.getDefaultFillColor(eClass);
            if(color != null) {
                fColorsCache.put(eClass, new Color(color.getRGB()));
            }
        }
        
        // Note Fill Color
        EClass eClass = IArchimatePackage.eINSTANCE.getDiagramModelNote();
        Color color = useInbuiltDefaults ? ColorFactory.getInbuiltDefaultFillColor(eClass) : ColorFactory.getDefaultFillColor(eClass);
        if(color != null) {
            fColorsCache.put(eClass, new Color(color.getRGB()));
        }
        
        // Group Fill Color
        eClass = IArchimatePackage.eINSTANCE.getDiagramModelGroup();
        color = useInbuiltDefaults ? ColorFactory.getInbuiltDefaultFillColor(eClass) : ColorFactory.getDefaultFillColor(eClass);
        if(color != null) {
            fColorsCache.put(eClass, new Color(color.getRGB()));
        }
        
        // Element line color - use any ArchiMate eClass as there is only one line color pref
        eClass = IArchimatePackage.eINSTANCE.getBusinessActor();
        color = useInbuiltDefaults ? ColorFactory.getInbuiltDefaultLineColor(eClass) : ColorFactory.getDefaultLineColor(eClass);
        if(color != null) {
            fColorsCache.put(DEFAULT_ELEMENT_LINE_COLOR, new Color(color.getRGB()));
        }

        // Connection line color - use this eClass as there is only one line color pref
        eClass = IArchimatePackage.eINSTANCE.getDiagramModelConnection();
        color = useInbuiltDefaults ? ColorFactory.getInbuiltDefaultLineColor(eClass) : ColorFactory.getDefaultLineColor(eClass);
        if(color != null) {
            fColorsCache.put(DEFAULT_CONNECTION_LINE_COLOR, new Color(color.getRGB()));
        }
        
        // Folder colours
        for(FolderType folderType : FolderType.VALUES) {
            if(folderType != FolderType.USER) { // This is not used
                color = useInbuiltDefaults ? FolderUIProvider.getDefaultFolderColor(folderType) : FolderUIProvider.getFolderColor(folderType);
                if(color != null) {
                    fColorsCache.put(folderType, new Color(color.getRGB()));
                }
            }
        }
        
        // Theme colors
        for(String colorId : themeColors) {
            RGB rgb = useInbuiltDefaults ? ThemeUtils.getDefaultThemeColor(colorId) : ThemeUtils.getCurrentThemeColor(colorId);
            if(rgb != null) {
                fColorsCache.put(colorId, new Color(rgb));
            }
        }
    }
    
    @Override
    public boolean performOk() {
        getPreferenceStore().setValue(DERIVE_ELEMENT_LINE_COLOR, fDeriveElementLineColorsButton.getSelection());
        getPreferenceStore().setValue(SAVE_USER_DEFAULT_COLOR, fPersistUserDefaultColors.getSelection());
        
        saveColors(getPreferenceStore(), true);
        saveThemeColors();
        
        return true;
    }
    
    @Override
    protected void performDefaults() {
        fDeriveElementLineColorsButton.setSelection(getPreferenceStore().getDefaultBoolean(DERIVE_ELEMENT_LINE_COLOR));
        fPersistUserDefaultColors.setSelection(getPreferenceStore().getDefaultBoolean(SAVE_USER_DEFAULT_COLOR));
        
        // Set color cache to inbuilt defaults
        resetColorsCache(true);
        
        // Clear tree image registry
        fImageRegistry.dispose();
        fImageRegistry = new ImageRegistry();
        
        // Update tree
        for(Entry<Object, Color> entry : fColorsCache.entrySet()) {
            fTreeViewer.update(entry.getKey(), null);
        }

        super.performDefaults();
    }
    
    /**
     * @throws IOException
     * Import a User color scheme
     */
    private void importUserColors() throws IOException {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setText(Messages.ColoursPreferencePage_22);
        
        if(!PlatformUtils.isMac()) { // Single file filtering in the Open dialog doesn't work on Mac
            dialog.setFilterExtensions(new String[] { "ArchiColours.prefs", "*.prefs", "*.*" } );  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            dialog.setFileName("ArchiColours.prefs"); //$NON-NLS-1$
        }
        else {
            dialog.setFilterExtensions(new String[] { "*.prefs", "*.*" } );  //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        String path = dialog.open();
        if(path == null) {
            return;
        }
        
        PreferenceStore store = new PreferenceStore(path);
        store.load();

        // Fill Colors / Folder Colors
        for(Entry<Object, Color> entry : fColorsCache.entrySet()) {
            String key = DEFAULT_FILL_COLOR_PREFIX + getColorKey(entry.getKey());
            String value = store.getString(key);
            
            if(StringUtils.isSet(value)) {
                setColor(entry.getKey(), ColorFactory.convertStringToRGB(value));
            }
            
            key = FOLDER_COLOUR_PREFIX + getColorKey(entry.getKey());
            value = store.getString(key);
            
            if(StringUtils.isSet(value)) {
                setColor(entry.getKey(), ColorFactory.convertStringToRGB(value));
            }
        }
        
        // Element Line Color
        String key = DEFAULT_ELEMENT_LINE_COLOR;
        String value = store.getString(key);
        if(StringUtils.isSet(value)) {
            setColor(key, ColorFactory.convertStringToRGB(value));
        }
        
        // Connection Line Color
        key = DEFAULT_CONNECTION_LINE_COLOR;
        value = store.getString(key);
        if(StringUtils.isSet(value)) {
            setColor(key, ColorFactory.convertStringToRGB(value));
        }
    }
    
    /**
     * @throws IOException
     * Export a user color scheme
     */
    private void exportUserColors() throws IOException {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        dialog.setText(Messages.ColoursPreferencePage_23);
        dialog.setFileName("ArchiColours.prefs"); //$NON-NLS-1$
        
        // Set to true for consistency on all OSs
        dialog.setOverwrite(true);
        
        String path = dialog.open();
        if(path == null) {
            return;
        }
        
        PreferenceStore store = new PreferenceStore(path);
        saveColors(store, false);
        store.save();
    }
    
    /**
     * @param store
     * @param useDefaults If true then a color is not saved if it matches the default color
     * Save colors to preference store
     */
    private void saveColors(IPreferenceStore store, boolean useDefaults) {
        for(Entry<Object, Color> entry : fColorsCache.entrySet()) {
            Color colorNew = entry.getValue();
            Color colorDefault = null;
            String key = null;
            
            // Element line color - use any ArchiMate eClass as there is only one line color pref
            if(entry.getKey().equals(DEFAULT_ELEMENT_LINE_COLOR)) {
                colorDefault = ColorFactory.getInbuiltDefaultLineColor(IArchimatePackage.eINSTANCE.getBusinessActor());
                key = DEFAULT_ELEMENT_LINE_COLOR;
            }
            // Connection line color - use this eClass as there is only one line color pref
            else if(entry.getKey().equals(DEFAULT_CONNECTION_LINE_COLOR)) {
                colorDefault = ColorFactory.getInbuiltDefaultLineColor(IArchimatePackage.eINSTANCE.getDiagramModelConnection());
                key = DEFAULT_CONNECTION_LINE_COLOR;
            }
            // Folders
            else if(entry.getKey() instanceof FolderType) {
                colorDefault = FolderUIProvider.getDefaultFolderColor((FolderType)entry.getKey());
                key = FOLDER_COLOUR_PREFIX + getColorKey(entry.getKey());
            }
            // EClass
            else if(entry.getKey() instanceof EClass) {
                colorDefault = ColorFactory.getInbuiltDefaultFillColor(entry.getKey());
                key = DEFAULT_FILL_COLOR_PREFIX + getColorKey(entry.getKey());
            }
            
            if(key != null) {
                // If the new color equals the default color set pref to default if useDefaults is true
                if(useDefaults && colorNew.equals(colorDefault)) {
                    store.setToDefault(key);
                }
                // Else store color anyway
                else {
                    store.setValue(key, ColorFactory.convertColorToString(colorNew));
                }
            }
        }
    }
    
    /**
     * Save any workbench theme colors
     */
    private void saveThemeColors() {
        try {
            // Remove listener so we are not notified
            PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeChangeListener);
            
            boolean themeResetNeeded = false;
            
            for(String colorId : themeColors) {
                Color color = fColorsCache.get(colorId);
                if(color != null && !color.getRGB().equals(ThemeUtils.getCurrentThemeColor(colorId))) {
                    ThemeUtils.setCurrentThemeColor(colorId, color.getRGB());
                    themeResetNeeded = true;
                }
            }
            
            if(themeResetNeeded) {
                ThemeUtils.resetCurrentTheme();
            }
        }
        finally {
            // Add listener again
            PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeChangeListener);
        }
    }
    
    @Override
    public void init(IWorkbench workbench) {
        // Listen to external theme/color changes to update our theme colors to match current theme.
        // A side effect is that we trigger this event when setting a theme color so remove our listener when setting theme colors.
        themeChangeListener = event -> {
            if(themeColors.contains(event.getProperty())) {
                RGB newValue = ThemeUtils.getCurrentThemeColor(event.getProperty());
                setColor(event.getProperty(), newValue);
            }
        };

        workbench.getThemeManager().addPropertyChangeListener(themeChangeListener);
    }
    
    @Override
    public void dispose() {
        super.dispose();
        
        fColorsCache.clear();
        fColorsCache = null;
        
        fImageRegistry.dispose();
        fImageRegistry = null;
        
        PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeChangeListener);
    }
}