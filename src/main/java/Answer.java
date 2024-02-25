public class Answer {
    String questionId;
    String answer1;

    @Override
    public String toString() {
        return "{\"questionId\":\"" + questionId + "\",\"answer1\":\"" + answer1 + "\"}";
    }

    public Answer(String questionId, String answer1) {
        this.questionId = questionId;
        this.answer1 = answer1;
    }

    public String getAnswer1() {
        return answer1;
    }

    public void setAnswer1(String answer1) {
        this.answer1 = answer1;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }
}
