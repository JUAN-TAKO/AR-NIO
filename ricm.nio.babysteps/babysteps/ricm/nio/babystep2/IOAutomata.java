package ricm.nio.babystep2;

public class IOAutomata{
	public ReaderAutomata reader;
	public WriterAutomata writer;
	public IOAutomata(ReaderAutomata r, WriterAutomata w) {
		reader = r;
		writer = w;
	}
	
}