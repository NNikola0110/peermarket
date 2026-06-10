package cli.command;

public interface CLICommand {

	String commandName();

	void execute(String args);
}
