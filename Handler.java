package prova.b;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class Handler implements Runnable {
	
	private Socket s;
	private Semaphore mutex;
	private HashMap<String, String> commands;
	private int clientNumber;
	private boolean quit;
	private PrintWriter out;
	private BufferedReader in;
	private static int clientTimeoutSeconds = 300;
	private Server server;
	private Instant timeAtStart;
	
	public Handler(Socket s, Semaphore mutex, HashMap<String, String> commands, int clientNumber, Server server) {
		this.s = s;
		this.mutex = mutex;
		this.commands = commands;
		this.clientNumber = clientNumber;
		this.quit = false;
		this.server = server;
		this.timeAtStart = Instant.now();
		try {
			s.setSoTimeout(this.clientTimeoutSeconds * 1000);
			this.out = new PrintWriter(s.getOutputStream(), true);
			this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			
			out.println("Client n. " + this.clientNumber + " (" + s.getInetAddress() + " - " + s.getPort() + ")");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void shutdownMessage(int seconds) {
		if(seconds == 0) {
			out.println("Server shutting down now!");
			try {
				quit = true;
				s.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			out.println("Server shutting in " + seconds + " seconds!");
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!quit) {
			try {
				String inputMsg = in.readLine();
				
				if(server.isShutdown() || inputMsg == null) {
					break;
				}
				inputMsg = inputMsg.toLowerCase();
				
				if(inputMsg.startsWith("help") && inputMsg.split(" ").length == 2 && commands.containsKey(inputMsg.split(" ")[1])){
					this.out.println("Description: " +  commands.get(inputMsg.split(" ")[1]));
				}
				else if(inputMsg.equalsIgnoreCase("help")) {
					out.println("Use help <command>");
				}
				else if(inputMsg.equalsIgnoreCase("cmdlist")) {
					out.println("Available commands: ");
					for(String command : commands.keySet()) {
						out.println(command);
					}
				}
				else if(inputMsg.equalsIgnoreCase("exit")) {
					
					out.println("Goodbye! You were connected for " + Duration.between(this.timeAtStart, Instant.now()).toSeconds() + " seconds.");
					quit = true;
					
				}
				else if(inputMsg.equalsIgnoreCase("con")) {
					out.println("Connected clients: " + server.connectedClients());
				}
				else if(inputMsg.equalsIgnoreCase("logaccept")) {
					out.println("Log:");
					server.printLog(this.out);
				}
				else if(inputMsg.startsWith("shutdown") && inputMsg.split(" ").length == 2 && inputMsg.split(" ")[1].matches("\\d+")) {
					server.shutdown(Integer.parseInt(inputMsg.split(" ")[1]));
				}
				else {
					out.println(new String(new StringBuilder(inputMsg).reverse()).toUpperCase());
				}
				

			} catch (IOException e) {
				if(quit)
					break;
				e.printStackTrace();
			}
			
		}
		try {
			server.remove(this);
			in.close();
			s.close();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
