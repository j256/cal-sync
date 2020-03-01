package com.j256.calsync;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttachment;
import com.google.api.services.calendar.model.EventDateTime;

/**
 * Class which wraps an event class which allows us to sort by date/time and find similar entries on the same day.
 * 
 * @author graywatson
 */
public class WrappedEvent implements Comparable<WrappedEvent> {

	private static final Double SUMMARY_SCORE_AWARD = 1.0;
	private static final Double LOCATION_SCORE_AWARD = 1.0;
	private static final Double DESCRIPTION_SCORE_AWARD = 1.0;
	private static final Double ATTACHMENTS_SCORE_AWARD = 1.0;
	private static final Double URLS_SCORE_AWARD = 1.0;
	private static final Double EMAIL_SCORE_AWARD = 1.0;
	private static final int WORD_OVERLAP_PERCENTAGE = 50;

	private static final int SUMMARY_BAD_NUM_WORDS = 2;
	private static final int SUMMARY_GOOD_NUM_WORDS = 5;
	private static final int LOCATION_BAD_NUM_WORDS = 2;
	private static final int LOCATION_GOOD_NUM_WORDS = 5;
	private static final int DESCRIPTION_BAD_NUM_WORDS = 10;
	private static final int DESCRIPTION_GOOD_NUM_WORDS = 40;

	private final Event event;
	private final long startTimeMillis;
	private final long endTimeMillis;
	private Double score;

	public WrappedEvent(Event event) {
		this.event = event;
		long firstStart = eventTimeToMillis(event.getStart());
		long firstEnd = eventTimeToMillis(event.getEnd());
		long maybeStart = eventTimeToMillis(event.getOriginalStartTime());
		if (maybeStart == 0) {
			this.startTimeMillis = firstStart;
			this.endTimeMillis = firstEnd;
		} else {
			this.startTimeMillis = maybeStart;
			this.endTimeMillis = maybeStart + (firstEnd - firstStart);
		}
	}

	/**
	 * Calculate and return a value score which is used to rate overlapping events on the calendar.
	 */
	public double calcScore() {
		if (score != null) {
			return score;
		}
		// title good size
		double val = 0.0F;
		// check summary words
		val += checkWords(SUMMARY_BAD_NUM_WORDS, SUMMARY_GOOD_NUM_WORDS, event.getSummary(), SUMMARY_SCORE_AWARD);
		// location number of words
		val += checkWords(LOCATION_BAD_NUM_WORDS, LOCATION_GOOD_NUM_WORDS, event.getLocation(), LOCATION_SCORE_AWARD);
		// description number of words
		String desc = event.getDescription();
		val += checkWords(DESCRIPTION_BAD_NUM_WORDS, DESCRIPTION_GOOD_NUM_WORDS, desc, DESCRIPTION_SCORE_AWARD);
		// at least 1 attachment
		List<EventAttachment> attachments = event.getAttachments();
		if (attachments != null && !attachments.isEmpty()) {
			val += ATTACHMENTS_SCORE_AWARD;
		}
		// description has at least 1 URL
		if (desc != null && !desc.isEmpty()) {
			if (desc.contains("http://") || desc.contains("https://")) {
				val += URLS_SCORE_AWARD;
			}
			if (desc.contains("mailto:")) {
				val += EMAIL_SCORE_AWARD;
			}
		}
		this.score = val;
		return val;
	}

	/**
	 * Returns true if the event seems to be the same event.
	 */
	public boolean isSimilar(WrappedEvent other) {
		// if start1 < start2 && end1 > start2 OR start1 > start2 && start1 < end2.
		if (!hasOverlappingTime(other)) {
			return false;
		}
		String summary1 = event.getSummary();
		String summary2 = other.event.getSummary();
		if (summary1 == null || summary1.isEmpty() || summary2 == null || summary2.isEmpty()) {
			return false;
		}
		if (summary1.equals(summary2)) {
			return true;
		}
		// see if one summary overlaps the other, in either order
		return (hasOverlappingWords(summary1, summary2) || hasOverlappingWords(summary2, summary1));
	}

	/**
	 * Returns true if the time overlaps another event else false.
	 */
	public boolean hasOverlappingTime(WrappedEvent other) {
		// if start1 < start2 && end1 > start2 OR start1 > start2 && start1 < end2.
		return (startTimeMillis <= other.startTimeMillis && endTimeMillis > other.startTimeMillis)
				|| (startTimeMillis > other.startTimeMillis && startTimeMillis < other.endTimeMillis);
	}

	public Event getEvent() {
		return event;
	}

	@Override
	public int compareTo(WrappedEvent other) {
		return Double.compare(calcScore(), other.calcScore());
	}

	@Override
	public String toString() {
		return event.toString();
	}

	private static long eventTimeToMillis(EventDateTime dateTime) {
		if (dateTime == null || dateTime.getDateTime() == null) {
			return 0;
		} else {
			return dateTime.getDateTime().getValue();
		}
	}

	private double checkWords(int bad, int good, String field, double award) {
		if (field == null || field.isEmpty()) {
			return 0.0;
		}
		String[] words = field.split("\\s");
		if (words.length < bad) {
			return 0.0;
		} else if (words.length >= good) {
			return award;
		} else {
			return award * (double) (words.length - bad) / (double) (good - bad);
		}
	}

	private static boolean hasOverlappingWords(String summary1, String summary2) {
		String[] words1 = summary1.split("\\s");
		String[] words2 = summary2.split("\\s");
		// see if we have 1/2 similar words in the summary
		Set<String> wordSet = new HashSet<>();
		for (String word : words1) {
			wordSet.add(word.toLowerCase());
		}
		int count = 0;
		for (String word : words2) {
			if (wordSet.contains(word.toLowerCase())) {
				count++;
			}
		}
		return ((count * 100 / wordSet.size()) > WORD_OVERLAP_PERCENTAGE);
	}
}
