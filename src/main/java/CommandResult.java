public class CommandResult {
    public int    result;
    public String successMsg;
    public String errorMsg;

    public CommandResult(int result){
        this.result = result;
    }

    public CommandResult(int result, String successMsg, String errorMsg){
        this.result = result;
        this.successMsg = successMsg;
        this.errorMsg = errorMsg;
    }
}
