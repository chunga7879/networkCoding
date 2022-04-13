package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

/**
 * Created by Jonatan on 2017-09-09.
 */
public class DictionaryConnection {

    private static final int DEFAULT_PORT = 2628;
    Socket dictSocket;
    PrintWriter out;
    BufferedReader in;

    /** Establishes a new connection with a DICT server using an explicit host and port number, and handles initial
     * welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @param port Port number used by the DICT server
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {
        try {
            dictSocket = new Socket(host, port);
            out = new PrintWriter(dictSocket.getOutputStream(), true);
            in = new BufferedReader(
                new InputStreamReader(dictSocket.getInputStream()));
            Status s = Status.readStatus(in);
            int statusCode = s.getStatusCode();

            System.out.println(statusCode + " " + s.getDetails());

            if (statusCode != 220) {
                throw new DictConnectionException();
            }

        } catch (IOException e) {
            throw new DictConnectionException();
        }
    }

    /** Establishes a new connection with a DICT server using an explicit host, with the default DICT port number, and
     * handles initial welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /** Sends the final QUIT message and closes the connection with the server. This function ignores any exception that
     * may happen while sending the message, receiving its reply, or closing the connection.
     *
     */
    public synchronized void close() {
        try {
            out.println("QUIT");
            String outMessage = in.readLine();
            String code = outMessage.substring(0, 3);

            if (code.equals("221")) {
                dictSocket.close();
                System.out.println(outMessage);
            } else {
                while ((outMessage = in.readLine()) != null) {
                    System.out.println(outMessage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Requests and retrieves all definitions for a specific word.
     *
     * @param word The word whose defin     * @param word The word whose definition is to be retrieved.ition is to be retrieved.
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 definitions in the first database that has a definition for the word should be used
     *                 (database '!').
     * @return A collection of Definition objects containing all definitions returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database) throws DictConnectionException {
        Collection<Definition> set = new ArrayList<>();
        out.println("DEFINE " + database.getName() + " " + word);

        try {
            String outMessage;

            Status s = Status.readStatus(in);
            int code = s.getStatusCode();
            System.out.println(s.getStatusCode() + " " + s.getDetails());

            if (code == 150) {
                int numOfDef = Integer.parseInt(s.getDetails().substring(0, 1));
                while (numOfDef > 0) {
                    s = Status.readStatus(in);
                    if (s.getStatusCode() == 151) {
                        String[] dicS = DictStringParser.splitAtoms(s.getDetails());
                        System.out.println(dicS[0] + " \"" + dicS[1] + "\" " + dicS[2]);
                        Definition def = new Definition(dicS[0], dicS[1]);
                        set.add(def);
                        while ((outMessage = in.readLine()) != null && !outMessage.equals(".")) {
                            def.appendDefinition(outMessage);
                            System.out.println(outMessage);
                        }
                        System.out.println(outMessage);
                    } else {
                        System.out.println(s.getStatusCode() + " " + s.getDetails());
                    }
                    numOfDef--;
                }
                outMessage = in.readLine();
                System.out.println(outMessage);
            } else if (code != 552 && code != 550) {
                throw new DictConnectionException();
            }
        } catch(IOException e) {
            throw new DictConnectionException();
        }
        return set;
    }

    /** Requests and retrieves a list of matches for a specific word pattern.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param strategy The strategy to be used to retrieve the list of matches (e.g., prefix, exact).
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 matches in the first database that has a match for the word should be used (database '!').
     * @return A set of word matches returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<String> getMatchList(String word, MatchingStrategy strategy, Database database) throws DictConnectionException {
        Set<String> set = new LinkedHashSet<>();
        out.println("MATCH " + database.getName() + " " + strategy.getName() + " " + word);

        String outMessage;

        Status s = Status.readStatus(in);
        int code = s.getStatusCode();
        System.out.println(s.getStatusCode() + " " + s.getDetails());

        try {
            if (code == 152) {
                while ((outMessage = in.readLine()) != null && !outMessage.equals(".")) {
                    System.out.println(outMessage);
                    String[] lists = DictStringParser.splitAtoms(outMessage);
                    set.add(lists[1]);
                }
                outMessage = in.readLine();
                System.out.println(outMessage);
            } else if (code != 552 && code != 550 && code != 551){
                throw new DictConnectionException();
            }
        } catch(IOException e) {
            throw new DictConnectionException();
        }
        return set;
    }

    /** Requests and retrieves a map of database name to an equivalent database object for all valid databases used in the server.
     *
     * @return A map of Database objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Map<String, Database> getDatabaseList() throws DictConnectionException {
        Map<String, Database> databaseMap = new HashMap<>();
        out.println("SHOW DATABASES");

        String outMessage;
        try {
            Status s = Status.readStatus(in);
            int code = s.getStatusCode();
            System.out.println(s.getStatusCode() + " " + s.getDetails());
            if (code == 110) {
                while ((outMessage = in.readLine()) != null && !outMessage.equals(".")) {
                    System.out.println(outMessage);
                    String[] dataB = DictStringParser.splitAtoms(outMessage);
                    Database b = new Database(dataB[0], dataB[1]);
                    databaseMap.put(b.getName(), b);
                }
                outMessage = in.readLine();
                System.out.println(outMessage);
            } else if (code != 554) {
                throw new DictConnectionException();
            }
        } catch(IOException e) {
            throw new DictConnectionException();
        }

        return databaseMap;
    }

    /** Requests and retrieves a list of all valid matching strategies supported by the server.
     *
     * @return A set of MatchingStrategy objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        Set<MatchingStrategy> set = new LinkedHashSet<>();
        out.println("SHOW STRATEGIES");
        String outMessage;
        try {
            Status s = Status.readStatus(in);
            int code = s.getStatusCode();
            System.out.println(s.getStatusCode() + " " + s.getDetails());
            if (code == 111) {
                while ((outMessage = in.readLine()) != null && !outMessage.equals(".")) {
                    System.out.println(outMessage);
                    String[] dataS = DictStringParser.splitAtoms(outMessage);
                    MatchingStrategy ms = new MatchingStrategy(dataS[0], dataS[1]);
                    set.add(ms);
                }
                outMessage = in.readLine();
                System.out.println(outMessage);
            } else if (code != 555) {
                throw new DictConnectionException();
            }
        } catch(IOException e) {
            throw new DictConnectionException();
        }
        return set;
    }
}

