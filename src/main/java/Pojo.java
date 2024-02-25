public class Pojo {
    String startDate;
    String submitDate;
    String answers;

    public Pojo(String startDate, String submitDate, String answers) {
        this.startDate = startDate;
        this.submitDate = submitDate;
        this.answers = answers;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(String submitDate) {
        this.submitDate = submitDate;
    }

    public String getAnswers() {
        return answers;
    }

    public void setAnswers(String answers) {
        this.answers = answers;
    }
}
