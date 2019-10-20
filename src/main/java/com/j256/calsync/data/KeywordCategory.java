package com.j256.calsync.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Keywords that appear in event descriptions that map to a category. Example: "gvpcal". Probably should be non-known
 * words. For example, we would not want the word "food" to always mean food-insecurity incase the description of a
 * gun-violence event happens to include the words "food will be provided".
 */
@DatabaseTable
public class KeywordCategory {

	@DatabaseField
	private String keyword;
	@DatabaseField
	private String category;

	public KeywordCategory() {
		// for ormlite
	}

	public KeywordCategory(String keyword, String category) {
		this.keyword = keyword;
		this.category = category;
	}

	public String getKeyword() {
		return keyword;
	}

	public String getCategory() {
		return category;
	}
}
