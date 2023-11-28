package edu.ncsu.csc.itrust2.api;

import edu.ncsu.csc.itrust2.common.TestUtils;
import edu.ncsu.csc.itrust2.forms.AppointmentRequestForm;
import edu.ncsu.csc.itrust2.forms.OfficeVisitForm;
import edu.ncsu.csc.itrust2.forms.UserForm;
import edu.ncsu.csc.itrust2.models.BasicHealthMetrics;
import edu.ncsu.csc.itrust2.models.Hospital;
import edu.ncsu.csc.itrust2.models.OfficeVisit;
import edu.ncsu.csc.itrust2.models.Patient;
import edu.ncsu.csc.itrust2.models.Personnel;
import edu.ncsu.csc.itrust2.models.User;
import edu.ncsu.csc.itrust2.models.enums.AppointmentType;
import edu.ncsu.csc.itrust2.models.enums.BloodType;
import edu.ncsu.csc.itrust2.models.enums.Ethnicity;
import edu.ncsu.csc.itrust2.models.enums.Gender;
import edu.ncsu.csc.itrust2.models.enums.HouseholdSmokingStatus;
import edu.ncsu.csc.itrust2.models.enums.PatientSmokingStatus;
import edu.ncsu.csc.itrust2.models.enums.Role;
import edu.ncsu.csc.itrust2.models.enums.State;
import edu.ncsu.csc.itrust2.models.enums.Status;
import edu.ncsu.csc.itrust2.services.AppointmentRequestService;
import edu.ncsu.csc.itrust2.services.BasicHealthMetricsService;
import edu.ncsu.csc.itrust2.services.HospitalService;
import edu.ncsu.csc.itrust2.services.OfficeVisitService;
import edu.ncsu.csc.itrust2.services.UserService;

import java.time.LocalDate;
import java.util.List;
import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for the API functionality for interacting with office visits
 *
 * @author Kai Presler-Marshall
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class APIOfficeVisitTest {

    private MockMvc mvc;

    @Autowired private WebApplicationContext context;

    @Autowired private OfficeVisitService officeVisitService;

    @Autowired private UserService userService;

    @Autowired private AppointmentRequestService appointmentRequestService;

    @Autowired private HospitalService hospitalService;

    @Autowired private BasicHealthMetricsService bhmService;

    /** Sets up test */
    @Before
    public void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();

        officeVisitService.deleteAll();

        appointmentRequestService.deleteAll();

        final User patient = new Patient(new UserForm("patient", "123456", Role.ROLE_PATIENT, 1));

        final User hcp = new Personnel(new UserForm("hcp", "123456", Role.ROLE_HCP, 1));
        final User hcp2 = new Personnel(new UserForm("hcp2", "123456", Role.ROLE_HCP, 1));

        final Patient antti = buildPatient();

        userService.saveAll(List.of(patient, hcp, hcp2, antti));

        final Hospital hosp =
                new Hospital("iTrust Test Hospital 2", "123 Raleigh Road", "27514", State.NC);

        hospitalService.save(hosp);
    }

    private Patient buildPatient() {
        final Patient antti = new Patient(new UserForm("antti", "123456", Role.ROLE_PATIENT, 1));

        antti.setAddress1("1 Test Street");
        antti.setAddress2("Some Location");
        antti.setBloodType(BloodType.APos);
        antti.setCity("Viipuri");
        final LocalDate date = LocalDate.of(1977, 6, 15);
        antti.setDateOfBirth(date);
        antti.setEmail("antti@itrust.fi");
        antti.setEthnicity(Ethnicity.Caucasian);
        antti.setFirstName("Antti");
        antti.setGender(Gender.Male);
        antti.setLastName("Walhelm");
        antti.setPhone("123-456-7890");
        antti.setState(State.NC);
        antti.setZip("27514");

        return antti;
    }

    /**
     * Tests getting a non existent office visit and ensures that the correct status is returned.
     */
    @Test
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testGetNonExistentOfficeVisit() throws Exception {
        mvc.perform(get("/api/v1/officevisits/-1")).andExpect(status().isNotFound());
    }

    /** Tests handling of errors when creating a visit for a pre-scheduled appointment. */
    @Test
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testPreScheduledOfficeVisit() throws Exception {

        final AppointmentRequestForm appointmentForm = new AppointmentRequestForm();

        // 2030-11-19 4:50 AM EST
        appointmentForm.setDate("2030-11-19T04:50:00.000-05:00");

        appointmentForm.setType(AppointmentType.GENERAL_CHECKUP.toString());
        appointmentForm.setStatus(Status.APPROVED.toString());
        appointmentForm.setHcp("hcp");
        appointmentForm.setPatient("patient");
        appointmentForm.setComments("Test appointment please ignore");

        appointmentRequestService.save(appointmentRequestService.build(appointmentForm));

        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setPreScheduled("yes");
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        mvc.perform(
                        post("/api/v1/officevisits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(TestUtils.asJsonString(visit)))
                .andExpect(status().isOk());

        Assert.assertEquals(1, officeVisitService.count());

        officeVisitService.deleteAll();

        Assert.assertEquals(0, officeVisitService.count());

        visit.setDate("2030-12-19T04:50:00.000-05:00");
        // setting a pre-scheduled appointment that doesn't match should not
        // work.
        mvc.perform(
                        post("/api/v1/officevisits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(TestUtils.asJsonString(visit)))
                .andExpect(status().isBadRequest());
    }

    /** Tests OfficeVisitAPI */
    @Test
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPI() throws Exception {

        Assert.assertEquals(0, officeVisitService.count());

        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Create the Office Visit */
        mvc.perform(
                        post("/api/v1/officevisits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(TestUtils.asJsonString(visit)))
                .andExpect(status().isOk());

        Assert.assertEquals(1, officeVisitService.count());

        mvc.perform(get("/api/v1/officevisits"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);
        List<OfficeVisit> vList = officeVisitService.findByHcp(v.getHcp());
        assertEquals(vList.get(0).getHcp(), v.getHcp());
        vList = officeVisitService.findByHcpAndPatient(v.getHcp(), v.getPatient());
        assertEquals(vList.get(0).getHcp(), v.getHcp());
        assertEquals(vList.get(0).getPatient(), v.getPatient());

        visit.setPatient("antti");
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        /* Test that all fields have been filled successfully */
        assertNotNull(v);
        assertEquals("antti", visit.getPatient());
        assertEquals(Integer.valueOf(83), visit.getDiastolic());
        assertEquals(Integer.valueOf(70), visit.getHdl());
        assertEquals(Float.valueOf(69.1f), visit.getHeight());
        assertEquals(HouseholdSmokingStatus.INDOOR, visit.getHouseSmokingStatus());
        assertEquals(Integer.valueOf(30), visit.getLdl());
        assertEquals(PatientSmokingStatus.FORMER, visit.getPatientSmokingStatus());
        assertEquals(Integer.valueOf(102), visit.getSystolic());
        assertEquals(Integer.valueOf(150), visit.getTri());
        assertEquals(Float.valueOf(175.2f), visit.getWeight());

        /* Create new BasicHealthMetrics for testing */
        final BasicHealthMetrics bhm1 = bhmService.build(visit);
        final BasicHealthMetrics bhm2 = bhmService.build(visit);
        assertEquals(bhm1, bhm1);
        assertEquals(bhm1, bhm2);
        assertEquals(bhm2, bhm1);
        assertNotEquals(bhm1, null);
        assertNotEquals(bhm1, v);

        /* Hash codes are the same for equal objects */
        assertEquals(bhm1.hashCode(), bhm2.hashCode());
        assertEquals(bhm1, bhm2);
        assertEquals(bhm2, bhm1);

        /* Diastolics are different */
        bhm2.setDiastolic(85);
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        /* One diastolic is null */
        bhm2.setDiastolic(null);
        bhm2.hashCode();
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm1.setDiastolic(null);
        assertEquals(bhm1,bhm2);
        bhm1.setDiastolic(83); 
        bhm2.setDiastolic(83);

        final User hcp2 = userService.findByName("hcp2");
        bhm2.setHcp(hcp2);
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm2.setHcp(null);
        bhm2.hashCode();
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm1.setHcp(null);
        assertEquals(bhm1,bhm2);
        final User hcp = userService.findByName("hcp");
        bhm2.setHcp(hcp);
        bhm1.setHcp(hcp);

        /* Weights are different */
        bhm2.setWeight(172.3f);
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);

        /* One weight is null */
        bhm2.setWeight(null);
        bhm2.hashCode();
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm1.setWeight(null);
        assertEquals(bhm1, bhm2);
        bhm2.setWeight(175.2f);
        bhm1.setWeight(175.2f);

        /* Tri is different */
        bhm2.setTri(140);
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);

        /* One tri is null */
        bhm2.setTri(null);
        bhm2.hashCode();
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm1.setTri(null);
        assertEquals(bhm1, bhm2);
        bhm2.setTri(150);
        bhm1.setTri(150);

        /* Systolics are different */
        bhm2.setSystolic(100);
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);

        /* One systolic is null */
        bhm2.setSystolic(null);
        bhm2.hashCode();
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm1.setSystolic(null);
        assertEquals(bhm1, bhm2);
        bhm2.setSystolic(102);
        bhm1.setSystolic(102);

        /* Patient smoking statuses are different */
        bhm2.setPatientSmokingStatus(PatientSmokingStatus.NEVER);
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm2.setPatientSmokingStatus(PatientSmokingStatus.FORMER);

        /* One patient is null */
        bhm2.setPatient(null);
        bhm2.hashCode();
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm1.setPatient(null);
        assertEquals(bhm1, bhm2);
        bhm2.setPatient(userService.findByName("antti"));
        bhm1.setPatient(userService.findByName("antti"));

        /* LDL's are different */
        bhm2.setLdl(40);
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);

        /* One LDL is null */
        bhm2.setLdl(null);
        bhm2.hashCode();
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm1.setLdl(null);
        assertEquals(bhm1, bhm2);
        bhm2.setLdl(30);
        bhm1.setLdl(30);

        /* Household smoking statuses are different */
        bhm2.setHouseSmokingStatus(HouseholdSmokingStatus.OUTDOOR);
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm2.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);

        /* Heights are different */
        bhm2.setHeight(60.2f);
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);

        /* One height is null */
        bhm2.setHeight(null);
        bhm2.hashCode();
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm1.setHeight(null);
        assertEquals(bhm1, bhm2);
        bhm2.setHeight(69.1f);
        bhm1.setHeight(69.1f);

        /* Different head circumferences */
        bhm2.setHeadCircumference(8.7f);
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm1.setHeadCircumference(8.7f);

        /* HDL's are different */
        bhm2.setHdl(80);
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);

        /* One HDL is null */
        bhm2.setHdl(null);
        bhm2.hashCode();
        assertNotEquals(bhm1, bhm2);
        assertNotEquals(bhm2, bhm1);
        bhm1.setHdl(null);
        assertEquals(bhm1, bhm2);
        bhm2.setHdl(70);
        bhm1.setHdl(70);

        assertEquals(bhm1, bhm2);
        assertEquals(bhm2, bhm1);

        /* Create appointment with patient younger than 12 years old */

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2020, 6, 15));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        v = officeVisitService.build(visit);
        assertNotNull(v);

        /* Create appointment with patient younger than 3 years old */
        final Patient patient3 = buildPatient();
        patient3.setDateOfBirth(LocalDate.of(2029, 6, 15));
        userService.save(patient3);
        visit.setHeadCircumference(20.0f);
        visit.setPatient(patient3.getUsername());
        v = officeVisitService.build(visit);
        assertNotNull(v);

        /*
         * We need the ID of the office visit that actually got _saved_ when
         * calling the API above. This will get it
         */
        final Long id =
                officeVisitService.findByPatient(userService.findByName("patient")).get(0).getId();

        visit.setId(id.toString());

        // Second post should fail with a conflict since it already exists
        mvc.perform(
                        post("/api/v1/officevisits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(TestUtils.asJsonString(visit)))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/v1/officevisits/" + id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        visit.setDate("2030-11-19T09:45:00.000-05:00");

        mvc.perform(
                        put("/api/v1/officevisits/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(TestUtils.asJsonString(visit)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        // PUT with ID not in database should fail
        final long tempId = 101;
        visit.setId("101");
        mvc.perform(
                        put("/api/v1/officevisits/" + tempId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(TestUtils.asJsonString(visit)))
                // .andExpect(status().isNotFound());
                .andExpect(status().is4xxClientError());

        vList.get(0).setId(0L);
        OfficeVisitForm vf = new OfficeVisitForm(vList.get(0));
    }

     /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError1() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        visit.setPatient("antti");
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setHdl(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError2() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        visit.setPatient("antti");
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setDiastolic(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError3() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        visit.setPatient("antti");
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setHeight(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError4() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        visit.setPatient("antti");
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setHouseSmokingStatus(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError5() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        visit.setPatient("antti");
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setLdl(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError6() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        visit.setPatient("antti");
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setSystolic(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError7() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        visit.setPatient("antti");
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setTri(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError8() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        visit.setPatient("antti");
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setWeight(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError9() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        visit.setPatient("antti");
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setPatientSmokingStatus(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError11() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2020, 6, 18));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setHeight(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError12() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2020, 6, 19));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setHouseSmokingStatus(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError13() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2020, 6, 21));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setSystolic(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError14() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2020, 6, 23));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setWeight(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError15() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2020, 6, 23));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        v = officeVisitService.build(visit);

        visit.setDiastolic(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError21() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2029, 7, 1));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        visit.setHeadCircumference(20.0f);
        v = officeVisitService.build(visit);

        visit.setHeadCircumference(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError22() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2029, 7, 1));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        visit.setHeadCircumference(20.0f);
        v = officeVisitService.build(visit);

        visit.setHouseSmokingStatus(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError23() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2029, 7, 1));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        visit.setHeadCircumference(20.0f);
        v = officeVisitService.build(visit);

        visit.setWeight(null);
        v = officeVisitService.build(visit);
    }

    /** Tests OfficeVisitAPI */
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError24() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2029, 7, 1));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(69.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        visit.setHeadCircumference(20.0f);
        v = officeVisitService.build(visit);

        visit.setHeight(null);
        v = officeVisitService.build(visit);
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError31() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2029, 7, 1));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(99999.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        visit.setHeadCircumference(20.0f);
        v = officeVisitService.build(visit);

        final BasicHealthMetrics bhm1 = bhmService.build(visit);
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError32() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2029, 7, 1));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(100.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(999999.2f);
        visit.setHeadCircumference(20.0f);
        v = officeVisitService.build(visit);

        final BasicHealthMetrics bhm1 = bhmService.build(visit);
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError33() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2029, 7, 1));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(100000);
        visit.setHdl(70);
        visit.setHeight(110.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        visit.setHeadCircumference(20.0f);
        v = officeVisitService.build(visit);

        final BasicHealthMetrics bhm1 = bhmService.build(visit);
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError34() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2029, 7, 1));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(110.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(10000);
        visit.setTri(150);
        visit.setWeight(175.2f);
        visit.setHeadCircumference(20.0f);
        v = officeVisitService.build(visit);

        final BasicHealthMetrics bhm1 = bhmService.build(visit);
    }
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError35() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2029, 7, 1));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(100);
        visit.setHeight(110.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        visit.setHeadCircumference(20.0f);
        v = officeVisitService.build(visit);

        final BasicHealthMetrics bhm1 = bhmService.build(visit);
    }
    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError36() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2029, 7, 1));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(110.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(11110000);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(150);
        visit.setWeight(175.2f);
        visit.setHeadCircumference(20.0f);
        v = officeVisitService.build(visit);

        final BasicHealthMetrics bhm1 = bhmService.build(visit);
    }

    @Test(expected = IllegalArgumentException.class)
    @Transactional
    @WithMockUser(
            username = "hcp",
            roles = {"HCP"})
    public void testOfficeVisitAPIError37() throws Exception {
        final OfficeVisitForm visit = new OfficeVisitForm();
        visit.setDate("2030-11-19T04:50:00.000-05:00");
        visit.setHcp("hcp");
        visit.setPatient("patient");
        visit.setNotes("Test office visit");
        visit.setType(AppointmentType.GENERAL_CHECKUP.toString());
        visit.setHospital("iTrust Test Hospital 2");

        /* Test getForHCP and getForHCPAndPatient */
        OfficeVisit v = officeVisitService.build(visit);

        final Patient patient2 = buildPatient();
        patient2.setDateOfBirth(LocalDate.of(2029, 7, 1));
        userService.save(patient2);
        visit.setPatient(patient2.getUsername());
        visit.setDiastolic(83);
        visit.setHdl(70);
        visit.setHeight(110.1f);
        visit.setHouseSmokingStatus(HouseholdSmokingStatus.INDOOR);
        visit.setLdl(30);
        visit.setPatientSmokingStatus(PatientSmokingStatus.FORMER);
        visit.setSystolic(102);
        visit.setTri(11100000);
        visit.setWeight(175.2f);
        visit.setHeadCircumference(20.0f);
        v = officeVisitService.build(visit);

        final BasicHealthMetrics bhm1 = bhmService.build(visit);
    }
}
