/*   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package compile.scomp.som.detailed;

import compile.scomp.som.common.SomTestBase;
import org.apache.xmlbeans.SchemaTypeSystem;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.*;


public class PartialSOMDetailedTest extends SomTestBase {

    // inherited methods
    @Before
    public void setUp() throws Exception {
        // initialize the built in schema type
        builtin = XmlBeans.getBuiltinTypeSystem();

        // populate the XmlOptions
        if (errors == null) {
            errors = new ArrayList();
        }
        if (options == null) {
            options = (new XmlOptions()).setErrorListener(errors);
            options.setCompileDownloadUrls();
            options.setCompilePartialTypesystem();
            options.setLoadLineNumbers();
        }

        // initialize the runid to be used for generating output files for the PSOM walk thro's
        runid = new Date().getTime();

        // clean the output from the previous run
        // delete directories created by checkPSOMSave() and output text file created by inspectPSOM()
        deleteDirRecursive(new File(somOutputRootDir));
    }

    @After
    public void tearDown() {
        errors.clear();
    }

    @Test
    public void testAddDataTypesList() {
        System.out.println("Inside test case testAddDataTypesList()");

        // Step 1 : create a PSOM from an incomplete/invalid xsd (datatypes.xsd) with unresolved references to various types
        String sBaseSourceName = "testsourcename";
        SchemaTypeSystem baseSTS = createNewSTS("datatypes.xsd_",
            null,
            "BaseSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", baseSTS);

        // recovearble errors should exist
        assertTrue("No Recovered Errors for Invalid Schema",
            printRecoveredErrors());

        // Walk thro the SOM (pass #Elems, #Attr, #Types, #AttrGroups)
        inspectSOM(baseSTS, 12, 1, 4, 1);

        // Test for saving of the PSOM - should not be able to save
        assertFalse("Partial SOM " + baseSTS.getName() + "Save successful - should fail!",
            checkPSOMSave(baseSTS));

        // instance validation - should fail
        assertFalse("Validation against instance Success - should fail ",
            validateInstance(getTestCaseFile("instance_datatypes_valid.xml"), baseSTS));

        // validate unresolved types - the ListType should resolve to 'anyType'
        assertEquals("Unresolved List Type should be 'anyType'",
            anyType,
            getElementType(baseSTS, "testListTypeElem"));
    }

    @Test
    public void testDeleteReusableGroups() {
        System.out.println("Inside test case testDeleteSubstitutionGroups()");

        // Step 1: read in a clean XSD groups_added.xsd
        String sBaseSourceName = "testsourcename";
        SchemaTypeSystem baseSTS = createNewSTS("reusable_grps_added.xsd_",
            null,
            "BaseSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", baseSTS);

        // there should be NO recovearble errors
        assertFalse("Recovered Errors for Valid Schema",
            printRecoveredErrors());

        // the tests - Walk thro the valid SOM
        inspectSOM(baseSTS, 7, 0, 5, 1);

        // Test for saving of the SOM - should go thro
        assertTrue("SOM " + baseSTS.getName() + "Save failed!",
            checkPSOMSave(baseSTS));

        // instance validation - should be ok
        assertTrue("Validation against instance failed",
            validateInstance(getTestCaseFile("instance_subst_grps_valid.xml"), baseSTS));

        // verify named model groups
        assertTrue("Attribute Group 'AttributeGroup' should exist",
            getAttributeGroup(baseSTS, "AttributeGroup"));
        assertTrue("Model Group 'NamedModelGroup' should exist",
            getModelGroup(baseSTS, "NamedModelGroup"));

        // step2: load an invalid PSOM by deleting the ModelGroup and AttributeGroup definitions commented
        SchemaTypeSystem modifiedSTS = createNewSTS("reusable_grps.xsd_",
            baseSTS,
            "ModifiedSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", modifiedSTS);

        // Recovered Errors, Test for saving of the SOM
        printRecoveredErrors();
        assertFalse("SOM " + modifiedSTS.getName() + "Save Success - should fail!",
            checkPSOMSave(modifiedSTS));

        // the tests - Walk thro the valid SOM
        inspectSOM(modifiedSTS, 7, 0, 5, 0);

        // instance validation - should fail
        assertFalse("Validation against instance failed",
            validateInstance(getTestCaseFile("instance_subst_grps_valid.xml"), modifiedSTS));

        // named model groups
        assertFalse("Attribute Group 'AttributeGroup' should not exist",
            getAttributeGroup(modifiedSTS, "AttributeGroup"));
        assertFalse("Model Group 'NamedModelGroup' should not exist",
            getModelGroup(modifiedSTS, "NamedModelGroup"));

        // step 3: create a PSOM with the original xsd
        SchemaTypeSystem finalSTS = createNewSTS("groups_added.xsd_",
            modifiedSTS,
            "FinalSchemaTS", sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", finalSTS);

        // Test for saving of the SOM - should go thro
        assertTrue("SOM " + finalSTS.getName() + "Save failed!",
            checkPSOMSave(finalSTS));

        // instance validation - should be ok
        assertTrue("Validation against instance failed",
            validateInstance(getTestCaseFile("instance_subst_grps_valid.xml"), finalSTS));

        // verify named model groups types
        assertTrue("Attribute Group 'AttributeGroup' should exist",
            getAttributeGroup(baseSTS, "AttributeGroup"));
        assertTrue("Model Group 'NamedModelGroup' should exist",
            getModelGroup(baseSTS, "NamedModelGroup"));

        // compare this to the original schema here
        assertTrue(compareSavedSOMs("BaseSchemaTS", "FinalSchemaTS"));
    }

    @Test
    public void testModifyDataTypesList() {
        System.out.println("Inside test case testModifyDataTypes()");

        // 1. remove one of the constituent types for the union and test to see if union is anySimpleType

        // Step 1: read in a clean XSD datatypes_added.xsd, to create a base schema with no unresolved components
        String sBaseSourceName = "testsourcename";
        SchemaTypeSystem baseSTS = createNewSTS("datatypes_added.xsd_",
            null,
            "BaseSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", baseSTS);

        // there should be NO recovearble errors
        assertFalse("Recovered Errors for Valid Schema",
            printRecoveredErrors());

        // the tests - Walk thro the valid SOM
        inspectSOM(baseSTS, 13, 1, 15, 1);

        // Recovered Errors, Test for saving of the SOM - should go thro
        assertTrue("SOM " + baseSTS.getName() + "Save failed!",
            checkPSOMSave(baseSTS));

        // instance validation - should be ok
        assertTrue("Validation against instance failed",
            validateInstance(getTestCaseFile("instance_datatypes_valid.xml"), baseSTS));

        // check types before modify
        assertEquals("Unresolved Simple Type should be 'attachmentTypes'",
            "attachmentTypes",
            getElementType(baseSTS, "testAtomicTypeElem"));
        assertEquals("Unresolved List Type should be 'attchmentExtensionListTypes'",
            "attchmentExtensionListTypes",
            getElementType(baseSTS, "testListTypeElem"));
        assertEquals("Unresolved Simple Type should be 'union.attachmentUnionType",
            "union.attachmentUnionType",
            getElementType(baseSTS, "testUnionTypeElem"));


        //Step 2 : modify types from the schema - should result in STS with unresolved refs
        SchemaTypeSystem modifiedSTS = createNewSTS("datatypes_modified.xsd_",
            baseSTS,
            "ModifiedSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", modifiedSTS);

        // test the PSOM created :walk thro the PSOM, look for # of elements,attributes,types & attribute groups
        inspectSOM(modifiedSTS, 13, 1, 13, 1);

        // Test for saving of the PSOM - should not be able to save
        assertFalse("PSOM " + modifiedSTS.getName() + " Save should fail",
            checkPSOMSave(modifiedSTS));

        // validate unresolved types
        // list and union types are of type "anyType" and not "anySimpleType
        // https://stackoverflow.com/questions/37801268/what-are-the-restrictions-of-xsdanysimpletype-on-xsdanytype-and-where-are-the
        assertEquals("Unresolved Simple Type - Atomic should be 'anyType'",
            anyType,
            getElementType(modifiedSTS, "testAtomicTypeElem"));
        assertEquals("Unresolved List Type should be 'anyType'",
            anyType,
            getElementType(modifiedSTS, "testListTypeElem"));
//        assertEquals("Unresolved Simple Type - Union should be 'anySimpleType'",
//            anySimpleType,
//            getElementType(modifiedSTS, "testUnionTypeElem"));

        // validate against an xml valid instance - should fail
        assertFalse("Validation against instance should Failed ",
            validateInstance(getTestCaseFile("instance_simple_types_invalid.xml"), modifiedSTS));

        // step 3: reload the original STS
        SchemaTypeSystem finalSTS = createNewSTS("datatypes_added.xsd_",
            modifiedSTS,
            "FinalSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", finalSTS);

        // walk the SOM
        inspectSOM(finalSTS, 13, 1, 15, 1);

        // validate successful save
        assertTrue("SOM " + finalSTS.getName() + "Save failed",
            checkPSOMSave(finalSTS)); // should be able to save as its a valid SOM

        // validate instance - should validate
        assertTrue("Validation against instance Failed ",
            validateInstance(getTestCaseFile("instance_simple_types_valid.xml"), finalSTS));

        // check types after modify
        assertEquals("Unresolved Simple Type should be 'attachmentTypes'",
            "attachmentTypes",
            getElementType(finalSTS, "testAtomicTypeElem"));
        assertEquals("Unresolved List Type should be 'attchmentExtensionListTypes'",
            "attchmentExtensionListTypes",
            getElementType(finalSTS, "testListTypeElem"));
        assertEquals("Unresolved Simple Type should be 'union.attachmentUnionType",
            "union.attachmentUnionType",
            getElementType(finalSTS, "testUnionTypeElem"));

        // compare this to the original schema here
        assertTrue(compareSavedSOMs("BaseSchemaTS", "FinalSchemaTS"));
    }

    @Test
    public void testDeleteDerivedTypes() {
        System.out.println("Inside test case testDeleteDerivedTypes()");

        // Step 1: read in a clean XSD derived_types_added.xsd with base and derived types to create a base schema with no unresolved components
        String sBaseSourceName = "testsourcename";
        SchemaTypeSystem baseSTS = createNewSTS("derived_types_added.xsd_",
            null,
            "BaseSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", baseSTS);

        // there should be NO recovearble errors
        assertFalse("Recovered Errors for Valid Schema",
            printRecoveredErrors());

        // the tests - Walk thro the valid SOM
        inspectSOM(baseSTS, 13, 0, 14, 0);

        // Recovered Errors, Test for saving of the SOM - should go thro
        assertTrue("Valid SOM " + baseSTS.getName() + "Save failed!",
            checkPSOMSave(baseSTS));

        // instance validation - should be ok
        assertTrue("Validation against instance failed",
            validateInstance(getTestCaseFile("instance_derived_types_valid.xml"), baseSTS));

        // check types before deletion of base types
        assertEquals("Elem Type  should be 'RestrictionSimpleContentBaseType'",
            "RestrictionSimpleContentBaseType", getElementType(baseSTS, "RestrictionSimpleContentBaseTypeElem"));

        // Step 2: create invalid PSOM with base type removed
        SchemaTypeSystem modifiedSTS = createNewSTS("derived_types.xsd_",
            baseSTS,
            "ModifiedSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", modifiedSTS);

        // recovearble errors
        assertTrue("No Recovered Errors for Invalid PSOM",
            printRecoveredErrors());

        // the tests - Walk thro the valid SOM
        inspectSOM(modifiedSTS, 13, 0, 9, 0);

        // Recovered Errors, Test for saving of the SOM
        assertFalse("SOM " + modifiedSTS.getName() + "Save Success - should fail!", checkPSOMSave(modifiedSTS));

        // instance validation - should fail
        assertFalse("Validation against instance failed",
            validateInstance(getTestCaseFile("instance_derived_types_valid.xml"), modifiedSTS));

        // check types - base should be 'anyType'
        // Restriction Complex Content Base type commented
        assertEquals("Elem Type  should be 'anyType'",
            anyType,
            getElementType(modifiedSTS, "RestrictionBaseComplexContentTypeElem"));


    }

    @Test
    public void testModifyReusableGroups() {
        System.out.println("Inside test case testModifyReusableGroups()");

        // Step 1: read in a clean XSD groups_added.xsd
        String sBaseSourceName = "testsourcename";
        SchemaTypeSystem baseSTS = createNewSTS("groups_added.xsd_",
            null,
            "BaseSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", baseSTS);

        // there should be NO recovearble errors
        assertFalse("Recovered Errors for Valid Schema",
            printRecoveredErrors());

        // the tests - Walk thro the valid SOM
        inspectSOM(baseSTS, 7, 0, 5, 2);

        // Test for saving of the SOM - should go thro
        assertTrue("SOM " + baseSTS.getName() + "Save failed!",
            checkPSOMSave(baseSTS));

        // instance validation - should be ok
        assertTrue("Validation against instance failed",
            validateInstance(getTestCaseFile("instance_subst_grps_valid.xml"), baseSTS));

        // verify named model groups
        assertTrue("Model Group 'NamedModelGroup' should exist ",
            getModelGroup(baseSTS, "NamedModelGroup"));
        assertTrue("Attribute Group 'AttributeGroup' should exist",
            getAttributeGroup(baseSTS, "AttributeGroup"));

        // step2: load a modified xsd with type of head elem in subs grp changed
        SchemaTypeSystem modifiedSTS = createNewSTS("reusable_grps_modified.xsd_",
            baseSTS,
            "ModifiedSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", modifiedSTS);

        // Recovered Errors, Test for saving of the SOM  , invalid since grp definitions are commented out
        printRecoveredErrors();
        assertFalse("SOM " + modifiedSTS.getName() + "Save Success - should fail!",
            checkPSOMSave(modifiedSTS));

        // the tests - Walk thro the valid SOM
        inspectSOM(modifiedSTS, 7, 0, 5, 1);

        // instance validation - should fail
        assertFalse("Validation against instance failed",
            validateInstance(getTestCaseFile("instance_subst_grps_valid.xml"), modifiedSTS));

        // verify named model groups
        assertEquals("Elem Type  should be 'ModelGrpType'",
            "ModelGrpType",
            getElementType(modifiedSTS, "ModelGrpTypeElem"));
        assertTrue("Elem Type  should be 'AttributeGroup'",
            getAttributeGroup(modifiedSTS, "AttributeGroup"));

        // step3 : reload the original xsd
        SchemaTypeSystem finalSTS = createNewSTS("groups_added.xsd_",
            modifiedSTS,
            "FinalSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", finalSTS);

        // Test for saving of the SOM - should go thro
        assertTrue("SOM " + finalSTS.getName() + "Save failed!",
            checkPSOMSave(finalSTS));

        // instance validation - should be ok
        assertTrue("Validation against instance failed",
            validateInstance(getTestCaseFile("instance_subst_grps_valid.xml"), finalSTS));

        // the tests - Walk thro the valid SOM
        inspectSOM(finalSTS, 7, 0, 5, 2);

        // verify named model groups
        assertEquals("Elem Type  should be 'ModelGrpType'",
            "ModelGrpType",
            getElementType(finalSTS, "ModelGrpTypeElem"));
        assertTrue("Elem Type  should be 'AttributeGroup'",
            getAttributeGroup(finalSTS, "AttributeGroup"));

        // compare this to the original schema here
        assertTrue(compareSavedSOMs("BaseSchemaTS", "FinalSchemaTS"));
    }

    @Test
    public void testModifyDerivedTypes() {
        System.out.println("Inside test case testModifyDerivedTypes()");

        // Step 1: read in a clean XSD derived_types_added.xsd
        String sBaseSourceName = "testsourcename";
        SchemaTypeSystem baseSTS = createNewSTS("derived_types_added.xsd_",
            null,
            "BaseSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", baseSTS);

        // there should be NO recovearble errors
        assertFalse("Recovered Errors for Valid Schema",
            printRecoveredErrors());

        // the tests - Walk thro the valid SOM
        inspectSOM(baseSTS, 13, 0, 14, 0);

        // Recovered Errors, Test for saving of the SOM - should go thro
        assertTrue("Valid SOM " + baseSTS.getName() + "Save failed!",
            checkPSOMSave(baseSTS));

        // instance validation - should be ok
        assertTrue("Validation against instance failed",
            validateInstance(getTestCaseFile("instance_derived_types_valid.xml"), baseSTS));

        // check types before deletion of base types
        assertEquals("Elem Type  should be 'ExtensionBaseType' (base)",
            "ExtensionBaseType",
            getElementType(baseSTS, "ExtensionBaseTypeElem"));
        assertEquals("Elem Type  should be 'ExtensionDerivedComplexContentType' (derived)",
            "ExtensionDerivedComplexContentType",
            getElementType(baseSTS, "ExtensionDerivedComplexContentTypeElem"));

        assertEquals("Elem Type  should be 'ExtensionBaseMixedContentType' (base)",
            "ExtensionBaseMixedContentType",
            getElementType(baseSTS, "ExtensionBaseMixedContentTypElem"));
        assertEquals("Elem Type  should be 'ExtensionDerivedMixedContentType' (derived)",
            "ExtensionDerivedMixedContentType",
            getElementType(baseSTS, "ExtensionDerivedMixedContentTypeElem"));

        assertEquals("Elem Type  should be 'RestrictionSimpleContentBaseType'",
            "RestrictionSimpleContentBaseType",
            getElementType(baseSTS, "RestrictionSimpleContentBaseTypeElem"));
        assertEquals("Elem Type  should be 'RestrictionSimpleContentDerivedType'",
            "RestrictionSimpleContentDerivedType",
            getElementType(baseSTS, "RestrictionSimpleContentDerivedTypeElem"));

        assertEquals("Elem Type  should be 'RestrictionBaseComplexContentType'",
            "RestrictionBaseComplexContentType",
            getElementType(baseSTS, "RestrictionBaseComplexContentTypeElem"));
        assertEquals("Elem Type  should be 'RestrictionDerivedComplexContentType'",
            "RestrictionDerivedComplexContentType",
            getElementType(baseSTS, "RestrictionDerivedComplexContentTypeElem"));

        assertEquals("Elem Type  should be 'RestrictionBaseMixedContentType'",
            "RestrictionBaseMixedContentType",
            getElementType(baseSTS, "RestrictionBaseMixedContentTypeElem"));
        assertEquals("Elem Type  should be 'RestrictionDerivedMixedContentType'",
            "RestrictionDerivedMixedContentType",
            getElementType(baseSTS, "RestrictionDerivedMixedContentTypeElem"));

        assertEquals("Elem Type  should be 'RestrictionBaseEmptyContentType'",
            "RestrictionBaseEmptyContentType",
            getElementType(baseSTS, "RestrictionBaseEmptyContentTypeElem"));
        assertEquals("Elem Type  should be 'RestrictionDerivedEmptyContentType'",
            "RestrictionDerivedEmptyContentType",
            getElementType(baseSTS, "RestrictionDerivedEmptyContentTypeElem"));


        // step 2 : change the base types now : derived_types_modified.xsd
        SchemaTypeSystem modifiedSTS = createNewSTS("derived_types_modifed.xsd_",
            baseSTS,
            "ModifiedSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", modifiedSTS);

        // no recovearble errors   just added another type
        assertFalse("valid PSOM",
            printRecoveredErrors());

        // the tests - Walk thro the valid SOM
        //inspectSOM(modifiedSTS, 13, 0, 14, 0);
        inspectSOM(modifiedSTS, 13, 0, 17, 0);

        // instance validation - should fail
        assertFalse("Validation against instance success - should fail",
            validateInstance(getTestCaseFile("instance_derived_types_valid.xml"), modifiedSTS));

        // now validate instance with new base type - this should go thro
        // TODO resolve     this validation
        //Assert.assertTrue("Validation against instance failed",
        //        validateInstance(getTestCaseFile("instance_derived_types_modify.xml"), modifiedSTS));

    }

    @Test
    public void testNameSpacesImportFile() {
        System.out.println("Inside test case testNameSpacesImportFile()");

        // Step 1: read in an xsd that imports from another xsd file providing file name only
        // The source name is not specified as this confuses the dereferecing of the location for the schemaLocation Attribute
        // The absolute rul specified in tbe basename (if specified) would also work.

        //String sBaseSourceName = "file:/D:/SVNNEW/xmlbeans/trunk/test/cases/xbean/compile/som/";
        SchemaTypeSystem baseSTS = createNewSTS("namespaces_import_fileonly.xsd_",
            null,
            "BaseSchemaTS",
            null);

        assertNotNull("Schema Type System created is Null.", baseSTS);

        // there should be NO recovearble errors   this should not be a partial Schema
        assertFalse("Recovered Errors for Valid Schema",
            printRecoveredErrors());
    }

    @Test
    public void testNameSpacesWithInclude() {
        System.out.println("Inside test case testNameSpacesWithInclude()");

        // Step 1: read in an xsd that includes another namespace in xsd file namespaces2.xsd
        //String sBaseSourceName = "testsourcename";
        SchemaTypeSystem baseSTS = createNewSTS("namespaces_include.xsd_",
            null,
            "BaseSchemaTS",
            null);

        assertNotNull("Schema Type System created is Null.", baseSTS);

        // there should be NO recovearble errors - this should not be a partial Schema
        assertFalse("Recovered Errors for Valid Schema",
            printRecoveredErrors());

        // Test for saving of the SOM - should go thro
        assertTrue("Valid SOM " + baseSTS.getName() + "Save failed!",
            checkPSOMSave(baseSTS));

        // the tests - Walk thro the valid SOM
        inspectSOM(baseSTS, 2, 0, 1, 0);


    }

    @Test
    @Ignore
    public void testNameSpacesImportFileWithPath() {
        System.out.println("Inside test case testNameSpacesImportFileWithPath()");

        //Step 1: read in an xsd that does not have any imports
        String sBaseSourceName = "testsourcename";
        SchemaTypeSystem baseSTS = createNewSTS("namespaces_noimports.xsd_",
            null,
            "BaseSchemaTS",
            sBaseSourceName);

        assertNotNull("Schema Type System created is Null.", baseSTS);

        // there should be NO recovearble errors - this should not be a partial Schema
        assertFalse("Recovered Errors for Valid Schema",
            printRecoveredErrors());

        // Test for saving of the SOM - should go thro
        assertTrue("Valid SOM " + baseSTS.getName() + "Save failed!",
            checkPSOMSave(baseSTS));

        // the tests - Walk thro the valid SOM
        inspectSOM(baseSTS, 1, 0, 0, 0);

        // step 2 : read in an xsd that imports a namespace from another xsd file providing the complete file path for the imported xsd
        SchemaTypeSystem modifiedSTS = createNewSTS("namespaces_import_filepath.xsd",
            baseSTS,
            "ModifiedSchemaTS",
            sBaseSourceName);
        assertNotNull("Schema Type System created is Null.", modifiedSTS);

        // no recovearble errors   just added another type
        assertFalse("valid PSOM",
            printRecoveredErrors());

        // the tests - Walk thro the valid SOM
        inspectSOM(modifiedSTS, 2, 0, 1, 0);

    }
}




