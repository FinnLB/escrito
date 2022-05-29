package de.unidue.ltl.escrito.io.essay;

import java.util.HashMap;
import java.util.Map;

public class MewsItem {

    private String DocumentID;
    private String essayText;
    private final Map<String, Rating> ratings;

    public MewsItem(String documentID, String essayText, Map<String, Rating> ratings) {
        DocumentID = documentID;
        this.essayText = essayText;
        this.ratings = ratings;

    }

    public String getDocumentID() {
        return DocumentID;
    }

    public void setDocumentID(String documentID) {
        DocumentID = documentID;
    }

    public String getEssayText() {
        return essayText;
    }

    public void setEssayText(String essayText) {
        this.essayText = essayText;
    }

    public Rating getRating(String category) {
        return ratings.get(category);
    }

    public void setRating(String category, int score, String rater) {
        ratings.put(category, new Rating(score, rater));
    }

    public String[] getCategories() {
        return ratings.keySet().toArray(new String[0]);
    }


    public static class Rating {
        private final int score;
        private final String rater;

        public Rating(int score, String rater) {
            this.score = score;
            this.rater = rater;
        }

        public int getScore() {
            return score;
        }

        public String getRater() {
            return rater;
        }
    }
}
