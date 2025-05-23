/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package com.archimatetool.editor.model.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.junit.jupiter.api.Test;

import com.archimatetool.editor.model.ModelChecker;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.util.ArchimateModelUtils;
import com.archimatetool.testingtools.ArchimateTestModel;
import com.archimatetool.tests.TestData;


/**
 * @author Phillip Beauvoir
 */
@SuppressWarnings("nls")
public class SetConceptTypeCommandFactoryTests {
    
    private ArchimateTestModel loadModel() throws IOException {
        ArchimateTestModel tm = new ArchimateTestModel(TestData.TEST_MODEL_FILE_ARCHISURANCE);
        tm.loadModelWithCommandStack();
        return tm;
    }
    
    @Test
    public void commandIsNullWhenElementTypeIsSame() {
        IArchimateModel model = IArchimateFactory.eINSTANCE.createArchimateModel();
        IArchimateElement element = IArchimateFactory.eINSTANCE.createBusinessActor();
        model.getDefaultFolderForObject(element).getElements().add(element);
        
        CompoundCommand command = SetConceptTypeCommandFactory.createSetElementTypeCommand(IArchimatePackage.eINSTANCE.getBusinessActor(), element, false);
        assertNull(command);
    }
    
    @Test
    public void commandIsNullWhenRelationTypeIsSame() {
        IArchimateModel model = IArchimateFactory.eINSTANCE.createArchimateModel();
        IArchimateElement element = IArchimateFactory.eINSTANCE.createBusinessActor();
        IArchimateRelationship relation = IArchimateFactory.eINSTANCE.createCompositionRelationship();
        relation.setSource(element);
        relation.setTarget(element);
        model.getDefaultFolderForObject(relation).getElements().add(relation);
        
        CompoundCommand command = SetConceptTypeCommandFactory.createSetRelationTypeCommand(IArchimatePackage.eINSTANCE.getCompositionRelationship(), relation);
        assertNull(command);
    }

    @Test
    public void commandIsNullWhenRelationTypeIsInvalid() {
        IArchimateModel model = IArchimateFactory.eINSTANCE.createArchimateModel();
        IArchimateElement element = IArchimateFactory.eINSTANCE.createBusinessActor();
        IArchimateRelationship relation = IArchimateFactory.eINSTANCE.createAssociationRelationship();
        relation.setSource(element);
        relation.setTarget(element);
        model.getDefaultFolderForObject(relation).getElements().add(relation);
        
        CompoundCommand command = SetConceptTypeCommandFactory.createSetRelationTypeCommand(IArchimatePackage.eINSTANCE.getAccessRelationship(), relation);
        assertNull(command);
    }
    
    @Test
    public void changeElementCommand() throws IOException {
        ArchimateTestModel tm = loadModel();
        
        // Business Role "Customer"
        IArchimateElement element = (IArchimateElement)tm.getObjectByID("521");
        assertNotNull(element);
        assertEquals(IArchimatePackage.eINSTANCE.getBusinessRole(), element.eClass());
        
        element.setDocumentation("Documentation");
        element.getProperties().add(IArchimateFactory.eINSTANCE.createProperty("key", "name"));
        element.getFeatures().putString("f", "1");
        
        // Store these now
        List<IArchimateRelationship> sourceRelations = new ArrayList<>(element.getSourceRelationships());
        List<IArchimateRelationship> targetRelations =  new ArrayList<>(element.getTargetRelationships());
        
        // Change to Application Component class
        EClass eClass = IArchimatePackage.eINSTANCE.getApplicationComponent();
        
        Command cmd = SetConceptTypeCommandFactory.createSetElementTypeCommand(eClass, element, false);
        assertNotNull(cmd);
        
        // Execute the command
        cmd.execute();
        testExecuteSetElementType(tm, eClass, element, sourceRelations, targetRelations);
        
        assertTrue(cmd.canRedo());
        assertTrue(cmd.canUndo());

        // Undo the command
        cmd.undo();
        
        // Model Check
        checkModel(tm.getModel());

        // Element should be back
        element = (IArchimateElement)tm.getObjectByID("521");
        assertNotNull(element);
        assertEquals(IArchimatePackage.eINSTANCE.getBusinessRole(), element.eClass());
        assertEquals("Customer", element.getName());
        assertEquals(sourceRelations.size(), element.getSourceRelationships().size());
        assertEquals(targetRelations.size(), element.getTargetRelationships().size());
        
        // Redo the command
        cmd.redo();
        testExecuteSetElementType(tm, eClass, element, sourceRelations, targetRelations);
    }
    
    private void testExecuteSetElementType(ArchimateTestModel tm, EClass eClass, IArchimateElement oldElement, List<IArchimateRelationship> sourceRelations, List<IArchimateRelationship> targetRelations) {
        // Model Check
        checkModel(tm.getModel());
        
        // Original element will be gone
        assertNull(tm.getObjectByID("521"));
        
        // The new element should be in the "Application" folder
        IFolder folder = tm.getModel().getFolder(FolderType.APPLICATION);
        assertEquals(1, folder.getElements().size());
        
        IArchimateElement newElement = (IArchimateElement)folder.getElements().get(0);
        
        assertEquals(eClass, newElement.eClass());
        assertEquals(oldElement.getName(), newElement.getName());
        assertNotEquals(oldElement.getId(), newElement.getId());
        assertEquals(oldElement.getDocumentation(), newElement.getDocumentation());
        assertTrue(EcoreUtil.equals(oldElement.getProperties(), newElement.getProperties()));
        assertTrue(EcoreUtil.equals(oldElement.getFeatures(), newElement.getFeatures()));
        assertTrue(oldElement.getReferencingDiagramObjects().isEmpty());
        assertTrue(oldElement.getSourceRelationships().isEmpty());
        assertTrue(oldElement.getTargetRelationships().isEmpty());
        
        assertEquals(sourceRelations.size(), newElement.getSourceRelationships().size());
        assertEquals(targetRelations.size(), newElement.getTargetRelationships().size());
        
        for(IArchimateRelationship relation : newElement.getSourceRelationships()) {
            assertTrue(ArchimateModelUtils.isValidRelationship(relation.getSource(), relation.getTarget(), relation.eClass()));
        }
        
        for(IArchimateRelationship relation : newElement.getTargetRelationships()) {
            assertTrue(ArchimateModelUtils.isValidRelationship(relation.getSource(), relation.getTarget(), relation.eClass()));
        }
    }
    
    @Test
    public void duplicateElementCommandsAreHandled() throws IOException {
        ArchimateTestModel tm = loadModel();
        
        // Business Role "Customer"
        IArchimateElement element = (IArchimateElement)tm.getObjectByID("521");
        assertNotNull(element);
        
        // Change to Application Component class
        EClass eClass = IArchimatePackage.eINSTANCE.getApplicationComponent();
        
        CompoundCommand cmd = SetConceptTypeCommandFactory.createSetElementTypeCommand(eClass, element, false);
        assertNotNull(cmd);
        
        // Should have sub-commands
        cmd.execute();
        assertEquals(2, cmd.getCommands().size());
        
        // Repeating the same thing should return null command
        cmd = SetConceptTypeCommandFactory.createSetElementTypeCommand(eClass, element, false);
        assertNull(cmd);
    }
    
    @Test
    public void duplicateRelationCommandsAreHandled() throws IOException {
        ArchimateTestModel tm = loadModel();
        
        // Triggering Relationship
        IArchimateRelationship relation = (IArchimateRelationship)tm.getObjectByID("95f13189");
        
        // Change to Flow Relationship
        EClass eClass = IArchimatePackage.eINSTANCE.getFlowRelationship();
        
        CompoundCommand cmd = SetConceptTypeCommandFactory.createSetRelationTypeCommand(eClass, relation);
        assertNotNull(cmd);
        
        // Should have sub-commands
        cmd.execute();
        assertEquals(4, cmd.getCommands().size());
        
        // Repeating the same thing should return a null command
        cmd = SetConceptTypeCommandFactory.createSetRelationTypeCommand(eClass, relation);
        assertNull(cmd);
    }
    
    @Test
    public void changeRelationshipCommand() throws IOException {
        ArchimateTestModel tm = loadModel();
        
        // Triggering Relation between "Customer" Business Role and "Request for Insurance" Business Event
        IArchimateRelationship relation = (IArchimateRelationship)tm.getObjectByID("95f13189");
        assertNotNull(relation);
        assertEquals(IArchimatePackage.eINSTANCE.getTriggeringRelationship(), relation.eClass());
        
        relation.setName("Test");
        relation.setDocumentation("Documentation");
        relation.getProperties().add(IArchimateFactory.eINSTANCE.createProperty("key", "name"));
        relation.getFeatures().putString("f", "1");
        
        IFolder parentFolder = (IFolder)relation.eContainer();
        IArchimateElement sourceElement = (IArchimateElement)relation.getSource();
        IArchimateElement targetElement = (IArchimateElement)relation.getTarget();
        
        // Change to Flow Relationship
        EClass eClass = IArchimatePackage.eINSTANCE.getFlowRelationship();
        
        CompoundCommand cmd = SetConceptTypeCommandFactory.createSetRelationTypeCommand(eClass, relation);
        assertNotNull(cmd);
        
        // Execute the command
        cmd.execute();
        testExecuteSetRelationType(tm, eClass, relation, sourceElement, targetElement, parentFolder);

        assertTrue(cmd.canRedo());
        assertTrue(cmd.canUndo());

        // Undo the command
        cmd.undo();
        
        // Model Check
        checkModel(tm.getModel());

        // Relation should be back
        relation = (IArchimateRelationship)tm.getObjectByID("95f13189");
        assertNotNull(relation);
        assertEquals(IArchimatePackage.eINSTANCE.getTriggeringRelationship(), relation.eClass());
        assertEquals("Test", relation.getName());
        
        // Redo the command
        cmd.redo();
        testExecuteSetRelationType(tm, eClass, relation, sourceElement, targetElement, parentFolder);
    }
    
    private void testExecuteSetRelationType(ArchimateTestModel tm, EClass eClass, IArchimateRelationship oldRelation, IArchimateElement oldSource, IArchimateElement oldTarget, IFolder parentFolder) {
        // Model Check
        checkModel(tm.getModel());

        // Original relation will be gone
        assertNull(tm.getObjectByID("770"));
        
        // The new element should be in the same folder at the last index position
        IArchimateRelationship newRelation = (IArchimateRelationship)parentFolder.getElements().get(parentFolder.getElements().size() - 1);
        
        assertEquals(eClass, newRelation.eClass());
        assertEquals(oldRelation.getName(), newRelation.getName());
        assertNotEquals(oldRelation.getId(), newRelation.getId());
        assertEquals(oldRelation.getDocumentation(), newRelation.getDocumentation());
        assertTrue(EcoreUtil.equals(oldRelation.getProperties(), newRelation.getProperties()));
        assertTrue(EcoreUtil.equals(oldRelation.getFeatures(), newRelation.getFeatures()));
        assertTrue(oldRelation.getReferencingDiagramConnections().isEmpty());
        
        assertSame(oldSource, newRelation.getSource());
        assertSame(oldTarget, newRelation.getTarget());
    }
    
    private void checkModel(IArchimateModel model) {
        ModelChecker checker = new ModelChecker(model);
        boolean ok = checker.checkAll();
        
        if(!ok) {
            for(String message : checker.getErrorMessages()) {
                System.out.println(message);
            }
            fail("Model Checker failed");
        }
    }
}