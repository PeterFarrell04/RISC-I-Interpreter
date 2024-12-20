import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Interpreter
{
    static int errorFlag = -1;
    static String errorString = "";
    static int [] reg;
    static HashMap<String, Integer> dict = new HashMap<String, Integer>();
    static int returnLine;
    static boolean mainFound = false;
    static int line = 0;
    public static void main(String[] args) {
        File file = new File("input.txt"); //to be replaced with user input

        setup();
        System.out.println("---START OF PROGRAM---\n");
        parse(file,line);
        System.out.println("\n---END OF PROGRAM---");
        debugPrintRegisters(0, 10);

    }

    public static void setup()
    {
        reg = new int[138];
        reg[0] = 0;
        returnLine = -1;
        mainFound = false;
        errorFlag = -1;
    }

    public static void parse(File f,int lineStart)
    {

        int l = -1;
        try {
            Scanner s = new Scanner(f);
            String data = "";
            while (s.hasNextLine() && l < lineStart)
            {
                data = s.nextLine();
                l++;
            }
            lex(data,l);

            System.out.println(data);
            if (errorFlag != -1)
            {
                System.out.println(errorString);
                return;
            }
            if (s.hasNextLine()) parse(f,l+1);
            else
            {
                if (!mainFound)
                {
                    mainFound = true;
                    if (dict.containsKey("main")) parse(f,dict.get("main")+1);
                    else {
                        setErrorProtocol(3,-1,null);
                        System.out.println(errorString);
                    }
                }
            }
        }catch(FileNotFoundException e)
        {
            //System.out.println(e);
        }

    }

    public static void lex(String data, int line)
    {
        if (data.contains(":"))
        {
            data = data.replace(":","");
            if (dict.containsKey(data))
            {
                setErrorProtocol(0,line,new String[]{data});
            }else
            {
                dict.put(data,line);
            }
            return;
        }
        if (!mainFound) return;
        if (data.toLowerCase().contains("add "))
        {
            data = data.replaceAll("(?i)add ", "");
            data = data.replace(" ", "");
            String[] args = data.split(",");
            if (args.length < 3)
            {
                setErrorProtocol(1,line,new String[]{"3", Integer.toString(args.length)});
                return;
            }
            int op1 = reg[getRegisterIndexFromString(args[0])];
            int op2 = StringToVal(args[1]);
            int destIndex = getRegisterIndexFromString(args[2]);
            updateRegister(destIndex,op1+op2);
            return;
        }

        if (!data.isEmpty()) {
            if (data.charAt(0) != ';')
            {
                setErrorProtocol(2,-1,new String[]{data});
            }
        }

    }

    public static void updateRegister(int index, int value)
    {
        //if (!dict.containsKey("main")) setErrorProtocol(3,-1,null);
        if (index > 0)
        {
            reg[index] = value;
        }
    }
    public static int getRegisterIndexFromString(String s)
    {
        if (s.toLowerCase().contains("r"))
        {
            s = s.replaceAll("(?i)r", "");
            return Integer.parseInt(s);
        }else
        {
            setErrorProtocol(2,-1,new String[]{s});
            return 0;
        }
    }
    public static int StringToVal(String s)
    {
        if (s.toLowerCase().contains("r")) {
            s = s.replaceAll("(?i)r", "");
            int register = Integer.parseInt(s);
            return reg[register];
        }
        if (s.toLowerCase().contains("#"))
        {
            s = s.replace("#","");
            return Integer.parseInt(s);
        }
        setErrorProtocol(2,-1,new String[]{"s"});
        return -1000; //replace with proper error handling
    }

    public static void debugPrintRegisters(int start,int end)
    {
        for (int i = start; i <= end; i++)
        {
            System.out.println("R" + i + " = " + reg[i] + ",");
        }
    }

    public static void setErrorProtocol(int flag, int line,String[] args)
    {
        errorFlag = flag;
        String e = "";
        if (line > -1) {
            e = "ERROR at Line " + (line + 1) + ": ";
        }else
        {
            e = "SYNTAX ERROR: ";
        }
        switch(errorFlag)
        {
            case 0:
                e+= "Tag \"" + args[0] + "\" Already Exists";
                break;
            case 1:
                e+= "Expected " + args[0] + " arguments but got " + args[1];
                break;
            case 2:
                e+= "Unknown Token \"" + args[0] + "\"";
                break;
            case 3:
                e+= "Unable to find start tag \"main\"";
                break;
            default:
                e += "Unkown Error";
                break;
        }

        errorString = e;
    }

}
