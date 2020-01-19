package com.j256.calsync.data;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Hashtag prefixes that appear in event descriptions that map to a category. Example: "#guns".
 */
@DatabaseTable
public class KeywordCategory extends BaseGeneratedIdEntity {

	@DatabaseField
	private String prefix;
	@DatabaseField(foreign = true, foreignAutoRefresh =  true)
	private Category category;

	public KeywordCategory() {
		// for ormlite
	}

	public KeywordCategory(String prefix, Category category) {
		this.prefix = prefix;
		this.category = category;
	}

	public String getPrefix() {
		return prefix;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	@Override
	public String toString() {
		return "KeywordCategory [prefix=" + prefix + ", cat=" + category + "]";
	}
}
