package jjug.attendee;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jjug.conference.Conference;
import jjug.submission.Submission;
import jjug.submission.SubmissionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendeeService {
	private final AttendeeRepository attendeeRepository;
	private final SubmissionRepository submissionRepository;

	public AttendeeService(AttendeeRepository attendeeRepository,
			SubmissionRepository submissionRepository) {
		this.attendeeRepository = attendeeRepository;
		this.submissionRepository = submissionRepository;
	}

	@Transactional
	public Attendee attend(String email, UUID confId, List<UUID> submissionIds) {
		return this.attendeeRepository.findByEmailAndConference_ConfId(email, confId) //
				.map(attendee -> this.updateAttendees(attendee, submissionIds)) //
				.orElseGet(() -> {
					List<Submission> submissions = submissionIds.isEmpty()
							? Collections.emptyList()
							: this.submissionRepository.findAll(submissionIds);
					Attendee attendee = this.attendeeRepository.save(Attendee.builder() //
							.email(email) //
							.submissions(submissions) //
							.conference(Conference.builder().confId(confId).build()) //
							.build());
					submissions.forEach(s -> s.getAttendees().add(attendee));
					return attendee;
				});
	}

	@Transactional
	public Attendee update(UUID attendeeId, String email, List<UUID> submissionIds) {
		return this.attendeeRepository.findByAttendeeId(attendeeId) //
				.map(attendee -> {
					this.updateAttendees(attendee, submissionIds);
					attendee.setEmail(email);
					return attendee;
				}).get();
	}

	private Attendee updateAttendees(Attendee attendee, List<UUID> submissionIds) {
		List<Submission> submissions = submissionIds.isEmpty() ? Collections.emptyList()
				: this.submissionRepository.findAll(submissionIds);
		attendee.getSubmissions().stream()
				.filter(s -> !submissionIds.contains(s.getSubmissionId()))
				.forEach(s -> s.getAttendees().remove(attendee));
		submissions.forEach(s -> s.getAttendees().add(attendee));
		attendee.setSubmissions(submissions);
		return attendee;
	}
}
