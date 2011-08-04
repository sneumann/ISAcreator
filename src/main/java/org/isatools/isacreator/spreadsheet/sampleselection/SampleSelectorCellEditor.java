package org.isatools.isacreator.spreadsheet.sampleselection;

import org.apache.commons.collections15.set.ListOrderedSet;
import org.isatools.isacreator.apiutils.StudyUtils;
import org.isatools.isacreator.autofilteringlist.FilterField;
import org.isatools.isacreator.common.UIHelper;
import org.isatools.isacreator.configuration.DataTypes;
import org.isatools.isacreator.configuration.FieldObject;
import org.isatools.isacreator.model.Factor;
import org.isatools.isacreator.model.Study;
import org.isatools.isacreator.ontologyselectiontool.OntologySelectionTool;
import org.isatools.isacreator.spreadsheet.Spreadsheet;
import org.isatools.isacreator.spreadsheet.SpreadsheetCellRange;
import org.isatools.isacreator.spreadsheet.TableReferenceObject;
import org.isatools.isacreator.spreadsheet.Utils;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.List;

public class SampleSelectorCellEditor extends FilterField implements TableCellEditor, DocumentListener, FocusListener, KeyListener {
    protected transient List<CellEditorListener> listeners;

    protected String originalValue;
    private SampleSelector selector;

    private JTable currentTable;
    private int currentRow;
    private int currentColumn;
    private Study study;
    private Spreadsheet spreadsheet;


    public SampleSelectorCellEditor(Spreadsheet spreadsheet) {
        super();

        this.spreadsheet = spreadsheet;
        this.study = spreadsheet.getStudyDataEntryEnvironment().getStudy();

        listeners = new ArrayList<CellEditorListener>();

        setForeground(UIHelper.DARK_GREEN_COLOR);
        setCaretColor(UIHelper.LIGHT_GREEN_COLOR);
        setSelectedTextColor(UIHelper.BG_COLOR);
        setSelectionColor(UIHelper.LIGHT_GREEN_COLOR);
        setBorder(null);
        addKeyListener(this);
    }

    /**
     * Add a CellEditorListener to the list of CelleEditorListeners available
     *
     * @param cel - A CellEditorListener to add
     */
    public void addCellEditorListener(CellEditorListener cel) {
        listeners.add(cel);
    }

    /**
     * CancelCellEditing method is required by the TableCellEditor interface.
     * Stops editing of the cell and hides the OntologySelectionTool.
     */
    public void cancelCellEditing() {
        fireEditingCanceled();
        selector.setVisible(false);
    }

    /**
     * FireEditingCanceled method sets text back to its original value, and cancels editing
     * by referring to the list of CellEditorListener objects and calling the editingStopped method with
     * a ChangeEvent defined using the OntologyCellEditor as a reference.
     */
    protected void fireEditingCanceled() {
        setText(originalValue);

        ChangeEvent ce = new ChangeEvent(this);

        for (int i = listeners.size() - 1; i >= 0; i--) {
            (listeners.get(i)).editingCanceled(ce);
        }


    }

    /**
     * Stops editing on a cell. Called when the user wants to commit the change rather than reverting to the
     * previous value entered.
     */
    protected void fireEditingStopped() {
        ChangeEvent ce = new ChangeEvent(this);

        for (int i = listeners.size() - 1; i >= 0; i--) {
            (listeners.get(i)).editingStopped(ce);
        }
    }

    /**
     * Return the value in the editor
     *
     * @return An object representing the value in the editor.
     */
    public Object getCellEditorValue() {
        return getText();
    }

    /**
     * Gets the component used to edit a cell - in this case, the OntologyCellEditor.
     *
     * @param table      - JTable being edited.
     * @param value      - /
     * @param isSelected - is the cell currently selected
     * @param row        - the row identifier for the cell
     * @param column     - the column identifier for the cell
     * @return The editing component. A OntologyCellEditor.
     */
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        currentTable = table;
        table.setRowSelectionInterval(row, row);
        table.setColumnSelectionInterval(column, column);

        if (table.getValueAt(row, column) != null) {
            originalValue = table.getValueAt(row, column).toString();
        } else {
            originalValue = "";
        }

        setText(originalValue);

        currentRow = row;
        currentColumn = column;

        return this;
    }

    private Point calculateDisplayLocation(JTable table, int row, int column) {

        Point p = table.getLocationOnScreen();
        Rectangle r = table.getCellRect(row, column, true);
        int proposedX = r.x + p.x;
        int proposedY = r.y + p.y + getHeight();

        Rectangle desktopBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();

        if ((proposedY + OntologySelectionTool.HEIGHT) > desktopBounds.height) {
            int difference = (proposedY + SampleSelector.HEIGHT) -
                    desktopBounds.height;
            proposedY = proposedY - difference;
        }

        return new Point(proposedX, proposedY);

    }

    /**
     * Always returns true since the date field should always be editable.
     *
     * @param eo EventObject
     * @return true, always.
     */
    public boolean isCellEditable(EventObject eo) {
        return true;
    }

    /**
     * Removes a CellEditorListener from the list of listeners.
     *
     * @param cel - CellEditorListener to remove
     */
    public void removeCellEditorListener(CellEditorListener cel) {
        listeners.remove(cel);
    }

    /**
     * Always returns true. Method indicates whether a user should be able to to select a cell.
     *
     * @param eo - EventObject.
     * @return true
     */
    public boolean shouldSelectCell(EventObject eo) {
        return true;
    }

    /**
     * Stop Editing of a cell component and set the cell value to be the term(s) selected in the OntologySelectionTool.
     *
     * @return true
     */
    public boolean stopCellEditing() {
        fireEditingStopped();
        if (selector != null) {
            selector.setVisible(false);
        }

        return true;
    }

    @Override
    public void changedUpdate(DocumentEvent event) {
        super.changedUpdate(event);

        showSelector();
    }

    @Override
    public void insertUpdate(DocumentEvent event) {
        super.insertUpdate(event);

        if (event.getType() != DocumentEvent.EventType.REMOVE) {
            showSelector();
        }

    }

    @Override
    public void removeUpdate(DocumentEvent event) {
        super.removeUpdate(event);

        if (selector != null) {
            if (selector.isShowing()) {

                if (getText().length() > 0) {
                    showSelector();
                } else {
                    hideSelector();
                }
            }
        }
    }

    private void showSelector() {

        if (selector == null) {
            instantiateSelectorIfRequired();
            selector.fadeInWindow();
        } else {
            selector.setLocation(calculateDisplayLocation(currentTable, currentRow, currentColumn));

            if (!selector.isShowing()) {
                selector.fadeInWindow();
            }

        }
    }

    private void instantiateSelectorIfRequired() {
        if (selector == null) {
            selector = new SampleSelector(this, StudyUtils.getStudySampleInformation(study));
            selector.createGUI();
            selector.setLocation(calculateDisplayLocation(currentTable, currentRow, currentColumn));
        }

        if (StudyUtils.isModified(study.getStudyId())) {
            System.out.println("Study samples for " + study.getStudyId() + " has been modified. Updating content");
            selector.updateContent(StudyUtils.getStudySampleInformation(study));
        }
    }

    private void propagateFactorsToAssay(String selectedSampleName) {

        System.out.println("PROPAGATING FACTORS!!");

        TableReferenceObject tableReferenceObject = study.getStudySample().getTableReferenceObject();

        List<FieldObject> recordedFactors = tableReferenceObject.getRecordedFactors();

        Map<String, SampleInformation> allSampleInformation = StudyUtils.getStudySampleInformation(study);

        Spreadsheet studySampleSheet = study.getStudySample().getSpreadsheetUI().getTable();

        Map<TableColumn, TableColumn> studySampleSheetFactors = studySampleSheet.getSpreadsheetFunctions().getFactors();

        // add columns and/or values
        for (FieldObject field : recordedFactors) {

            TableColumn factorColumn = null;
            TableColumn unitColumn = null;

            Map<TableColumn, TableColumn> assaySheetFactors = spreadsheet.getSpreadsheetFunctions().getFactors();

            for (TableColumn column : assaySheetFactors.keySet()) {
                if (column.getHeaderValue().equals(field.getFieldName())) {
                    factorColumn = column;
                    unitColumn = assaySheetFactors.get(column);
                }
            }

            if (factorColumn == null) {

                int factorIndex = spreadsheet.getColumnCount();

                FieldObject newFactor;
                if ((newFactor = spreadsheet.getTableReferenceObject().getFieldByName(field.getFieldName())) != null) {
                    newFactor = new FieldObject(factorIndex,
                            field.getFieldName(), field.getDescription(), field.getDatatype(),
                            field.getDefaultVal(), field.isRequired(), field.isAcceptsMultipleValues(), field.isAcceptsFileLocations());
                }

                spreadsheet.getSpreadsheetFunctions().addFieldToReferenceObject(newFactor);

                factorColumn = spreadsheet.getSpreadsheetFunctions().addColumn(newFactor.getFieldName());


                if (field.getDatatype() == DataTypes.STRING) {

                    FieldObject unitFo = new FieldObject(spreadsheet.getColumnCount(),
                            "Unit", "Unit for definition of value",
                            DataTypes.ONTOLOGY_TERM, "", false, false,
                            false);

                    spreadsheet.getSpreadsheetFunctions().addFieldToReferenceObject(unitFo);

                    unitColumn = spreadsheet.getSpreadsheetFunctions().addColumn("Unit");

                    spreadsheet.getSpreadsheetFunctions().addColumnToDependencies(factorColumn, unitColumn);

                }
            }

            // Study sample file
            //      1      2        3
            // FV [Dose] Unit FV [Compound]

            // Assay file
            //     3       4          5
            // FV [Dose] Unit FV [Compound]

            // Now we just need to map across since all the columns should be in place
            // We have the TableColumns from the StudySample File
            // We have the TableColumns from the Assay File.

            // add Factor value
            TableColumn associatedStudySampleFactor = null;

            System.out.println("There are " + studySampleSheetFactors.size() + " factors recognised...");

            for (TableColumn studySampleColumn : studySampleSheetFactors.keySet()) {
                System.out.println("Study sample column is :" + studySampleColumn.getHeaderValue());
                if (studySampleColumn.getHeaderValue().toString().equals(factorColumn.getHeaderValue().toString())) {
                    System.out.println("Found match, now getting value :" + factorColumn.getHeaderValue().toString());
                    // we have the first value
                    Object value = studySampleSheet.getTable().getValueAt(allSampleInformation.get(selectedSampleName).getRowNumber(),
                            Utils.convertModelIndexToView(studySampleSheet.getTable(), studySampleColumn.getModelIndex()));

                    spreadsheet.getTable().setValueAt(value == null ? "" : value.toString(), currentRow,
                            Utils.convertModelIndexToView(spreadsheet.getTable(), factorColumn.getModelIndex()));

                    if (studySampleSheetFactors.get(studySampleColumn) != null) {

                        Object unit = studySampleSheet.getTable().getValueAt(allSampleInformation.get(selectedSampleName).getRowNumber(),
                                Utils.convertModelIndexToView(studySampleSheet.getTable(), studySampleSheetFactors.get(studySampleColumn).getModelIndex()));

                        spreadsheet.getTable().setValueAt(unit == null ? "" : unit.toString(), currentRow,
                                Utils.convertModelIndexToView(spreadsheet.getTable(), unitColumn.getModelIndex()));
                    }

                    break;
                }
            }

            // add Unit

        }
    }


    private void hideSelector() {
        if (selector.isShowing()) {
            selector.fadeOutWindow();
        }
    }

    public void focusGained(FocusEvent focusEvent) {

    }

    public void focusLost(FocusEvent focusEvent) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                selector.fadeOutWindow();
            }
        });
    }

    public void keyTyped(KeyEvent keyEvent) {
    }

    public void keyPressed(KeyEvent keyEvent) {
        instantiateSelectorIfRequired();

        if (keyEvent.getKeyCode() == KeyEvent.VK_DOWN) {
            if (!selector.isShowing()) {
                showSelector();
            } else {
                selector.selectNextItem();
            }
        }

        if (keyEvent.getKeyCode() == KeyEvent.VK_UP) {
            if (!selector.isShowing()) {
                showSelector();
            } else {
                selector.selectPreviousItem();
            }
        }

        if (keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (selector.isShowing()) {
                hideSelector();
            }
        }

        if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {

            if (selector.getSelectedItem() != null) {
                setText(selector.getSelectedItem().toString());
            } else {
                setText(selector.getSelectedValue());
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    propagateFactorsToAssay(getText());
                }
            });
        }
    }

    public void keyReleased(KeyEvent keyEvent) {

    }


}
