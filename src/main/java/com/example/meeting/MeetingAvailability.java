package com.example.meeting;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.Data;

public class MeetingAvailability {

	public List<String> findAvailableTime(List<String> calendarIds, int duration, String periodToSearch,
			String timeSlotType) throws IOException {
		Map<String, List<Appointment>> calendarData = parseCalendarData(calendarIds);
		List<String> availableMeetingTimes = new ArrayList<>();

		String[] interval = periodToSearch.split("/");

		LocalDateTime startTime = LocalDateTime.parse(interval[0], DateTimeFormatter.ISO_DATE_TIME);
		LocalDateTime endTime = LocalDateTime.parse(interval[1], DateTimeFormatter.ISO_DATE_TIME);
		System.out.println("-------------------Input Start time and End Time----------------------------");
		System.out.println("Start Time: " + startTime);
		System.out.println("End Time: " + endTime);
		System.out.println("-------------------Available meeting times details--------------------------");

		for (Map.Entry<String, List<Appointment>> entry : calendarData.entrySet()) {
			List<Appointment> appointments = entry.getValue();
			for (Appointment appointment : appointments) {
				// Skip appointments that end before the specified start time
				if (appointment.getEnd().isBefore(startTime)) {
					continue;
				}

				// Skip appointments that start after the specified end time
				if (appointment.getStart().isAfter(endTime)) {
					break; // Assuming appointments are sorted by start time, no need to check remaining
							// appointments
				}

				// Adjust the slot start time and slot end time to be within the specified
				// period
				LocalDateTime slotStartTime = appointment.getStart().isAfter(startTime) ? appointment.getStart()
						: startTime;
				LocalDateTime slotEndTime = appointment.getEnd().isBefore(endTime) ? appointment.getEnd() : endTime;

				// Calculate the duration in minutes
				Duration slotDuration = Duration.between(slotStartTime, slotEndTime);

				// Check if the slot duration is sufficient for the meeting
				if (slotDuration.toMinutes() >= duration) {
					if (timeSlotType == null || timeSlotType.equals(appointment.getTime_slot_type_id())) {

						System.out.println("Appointment ID: " + appointment.getId());
						System.out.println("Patient ID: " + appointment.getPatient_id());
						System.out.println("Start Time: " + slotStartTime);
						System.out.println("End Time: " + slotStartTime.plusMinutes(duration));
						System.out.println(
								"----------------------------------------------------------------------------");

						availableMeetingTimes.add(formatDateString(slotStartTime) + "/"
								+ formatDateString(slotStartTime.plusMinutes(duration)));
					}
				}
			}
		}

		return availableMeetingTimes;
	}

	private String formatDateString(LocalDateTime dateTime) {
		return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
	}

	private Map<String, List<Appointment>> parseCalendarData(List<String> calendarIds) throws IOException {
		Map<String, List<Appointment>> calendarData = new HashMap<>();
		for (String calendarId : calendarIds) {
			List<Appointment> appointments = readAppointmentsFromJson(calendarId);
			calendarData.put(calendarId, appointments);
		}
		return calendarData;
	}

	private List<Appointment> readAppointmentsFromJson(String calendarId) throws IOException {
		String jsonData = readJsonDataFromFileOrApi(calendarId);

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		RootWrapper wrapper = objectMapper.readValue(jsonData, RootWrapper.class);

		return wrapper.getAppointments();
	}

	private String readJsonDataFromFileOrApi(String calendarId) {
		// Construct the file path based on the calendarId
		String filePath = "src/main/resources/" + calendarId + ".json";

		// Initialize a StringBuilder to store the JSON data
		StringBuilder jsonData = new StringBuilder();

		try {
			// Create a File object
			File file = new File(filePath);

			// Create a Scanner to read the file
			Scanner scanner = new Scanner(file);

			// Read JSON data line by line and append to the StringBuilder
			while (scanner.hasNextLine()) {
				jsonData.append(scanner.nextLine());
			}

			// Close the scanner
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace();
			// Handle exceptions appropriately
		}

		// Return the JSON data as a string
		return jsonData.toString();
	}

	public static void main(String[] args) throws IOException {
		List<String> calendarIds = Arrays.asList("Emma_Win");
		int meetingDuration = 15;
		String periodToSearch = "2019-04-23T08:00:00Z/2019-04-23T16:00:00Z";

		// Split the periodToSearch string
		String[] parts = periodToSearch.split("/");

		// Extract start and end time strings
		String startTime = parts[0].substring(0, parts[0].length() - 1);
		String endTime = parts[1].substring(0, parts[1].length() - 1);

		// Reconstruct the periodToSearch string without 'Z'
		periodToSearch = startTime + "/" + endTime;

		String timeSlotType = "452935de-975e-11e5-ae1a-c8e0eb18c1e9";

		MeetingAvailability meetingAvailability = new MeetingAvailability();
		List<String> availableMeetingTimes = meetingAvailability.findAvailableTime(calendarIds, meetingDuration,
				periodToSearch, timeSlotType);

		System.out.println("Available meeting times:");
		for (String time : availableMeetingTimes) {
			System.out.println(time);
		}
	}

}

@Data
class Appointment {
	public String id;
	public String patient_id;
	public String calendar_id;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
	public LocalDateTime start;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
	public LocalDateTime end;
	public Object patient_comment;
	public Object note;
	public String time_slot_type_id;
	public Object type_id;
	public int state;
	public String out_of_office_location;
	public boolean out_of_office;
	public boolean completed;
	public boolean is_scheduled;
}

class RootWrapper {
	private List<Appointment> appointments;
	private List<Timeslot> timeslots;
	private List<Timeslottype> timeslottypes;
	private PatientMeta patient_meta;

	public List<Appointment> getAppointments() {
		return appointments;
	}

	public void setAppointments(List<Appointment> appointments) {
		this.appointments = appointments;
	}

	public List<Timeslot> getTimeslots() {
		return timeslots;
	}

	public void setTimeslots(List<Timeslot> timeslots) {
		this.timeslots = timeslots;
	}

	public List<Timeslottype> getTimeslottypes() {
		return timeslottypes;
	}

	public void setTimeslottypes(List<Timeslottype> timeslottypes) {
		this.timeslottypes = timeslottypes;
	}

	public PatientMeta getPatient_meta() {
		return patient_meta;
	}

	public void setPatient_meta(PatientMeta patient_meta) {
		this.patient_meta = patient_meta;
	}
}

@Data
class Timeslot {
	public String id;
	public String calendar_id;
	public String type_id;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
	public LocalDateTime start;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
	public LocalDateTime end;
	public boolean public_bookable;
	public boolean out_of_office;
}

@Data
class Timeslottype {
	public String id;
	public String name;
	public int slot_size;
	public boolean public_bookable;
	public String color;
	public String icon;
	public String clinic_id;
	public Object deleted;
	public boolean out_of_office;
	public boolean enabled;
}

@Data
class PatientData {
	public String firstname;
	public Object middlename;
	public String lastname;
	public String personal_id;
	public String birth_date;
	public String gender;
	public ArrayList<Object> contacts;
}

@Data
class First {
	public String firstname;
	public String middlename;
	public String lastname;
	public String personal_id;
	public String birth_date;
	public String gender;
	public ArrayList<Contact> contacts;
}

@Data
class Address {
	public String address_type;
	public String address;
	public Object number;
	public String type;
	public int country_code;
	public String street;
	public int floor;
	public String identifier;
	public String postal_code;
	public String postal_area;
	public Object municipal;
	public Object county;
	public String country;
	public Object latitude;
	public Object longitude;
}

@Data
class Contact {
	public String location;
	public ArrayList<Address> addresses;
}

@Data
class PatientMeta {
	@JsonProperty("00000000-0000-4000-a001-000000000001")
	public First first;
	@JsonProperty("1b94f86e-9751-11e5-bbe0-c8e0eb18c1e9")
	public PatientData patientData1;
	@JsonProperty("1bae2f82-9751-11e5-940c-c8e0eb18c1e9")
	public PatientData patientData2;
	@JsonProperty("1bbbd45c-9751-11e5-a3c5-c8e0eb18c1e9")
	public PatientData patientData3;
	@JsonProperty("1bc93318-9751-11e5-9d96-c8e0eb18c1e9")
	public PatientData patientData4;
	@JsonProperty("1bdb8194-9751-11e5-a2e7-c8e0eb18c1e9")
	public PatientData patientData5;
	@JsonProperty("1bee2bf0-9751-11e5-bc1b-c8e0eb18c1e9")
	public PatientData patientData6;
	@JsonProperty("1c1809b6-9751-11e5-85f0-c8e0eb18c1e9")
	public PatientData patientData7;
	@JsonProperty("1c2dfb36-9751-11e5-b75d-c8e0eb18c1e9")
	public PatientData patientData8;
	@JsonProperty("1c4287ae-9751-11e5-8abd-c8e0eb18c1e9")
	public PatientData patientData9;
	@JsonProperty("1c87d958-9751-11e5-9a51-c8e0eb18c1e9")
	public PatientData patientData10;
	@JsonProperty("1c98afbc-9751-11e5-a5ff-c8e0eb18c1e9")
	public PatientData patientData11;
	@JsonProperty("1cabb83c-9751-11e5-9289-c8e0eb18c1e9")
	public PatientData patientData12;
	@JsonProperty("1cc65052-9751-11e5-8d84-c8e0eb18c1e9")
	public PatientData patientData13;
	@JsonProperty("1cd7c8d2-9751-11e5-822f-c8e0eb18c1e9")
	public PatientData patientData14;
	@JsonProperty("1cefeb10-9751-11e5-95e2-c8e0eb18c1e9")
	public PatientData patientData15;
	@JsonProperty("1cfeee58-9751-11e5-9c8d-c8e0eb18c1e9")
	public PatientData patientData16;
	@JsonProperty("1d248ab4-9751-11e5-b983-c8e0eb18c1e9")
	public PatientData patientData17;
}
