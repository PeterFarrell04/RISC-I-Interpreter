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
    static int gotoLine;
    static boolean mainFound = false;
    static boolean finalLine = false;
    static int line = 0;
    public static void main(String[] args) {
        File file = new File("input.txt"); //to be replaced with user input

        setup();
        System.out.println("---START OF PROGRAM---\n");
        parse(file,line);
        System.out.println("\n---END OF PROGRAM---");
        debugPrintRegisters(0, 15);

    }

    public static void setup()
    {
        reg = new int[138];
        reg[0] = 0;
        reg[31] = -10;
        gotoLine = -1;
        mainFound = false;
        errorFlag = -1;
    }

    public static void parse(File f,int lineStart)
    {
        if (mainFound) debugPrintRegisters(1,1);
        int nl = -1;

        if (gotoLine != -1)
        {
            nl = gotoLine;
            gotoLine = -1;
        }
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
            if (gotoLine < -1) finalLine = true;

            //System.out.println(data);
            if (errorFlag != -1)
            {
                System.out.println(errorString);
                return;
            }
            if (s.hasNextLine())
            {

                if (nl <= -1) parse(f,l+1);
                else parse(f,nl);
            }
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
                }else
                {
                    if (finalLine)
                    {
                        return;
                    }else
                    {
                        setErrorProtocol(4,-1,null);
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
        if (data.contains(";"))
        {
            data = data.substring(0,data.indexOf(';'));
        }
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
        if (data.toLowerCase().contains("callr "))
        {
            data = data.replaceAll("(?i)callr ", "");
            data = data.replace(" ", "");
            String[] args = data.split(",");

            if (args.length < 2)
            {
                setErrorProtocol(1,line,new String[]{"2", Integer.toString(args.length)});
                return;
            }
            int op1 = StringToVal(args[0]);
            int dest = getRegisterIndexFromString(args[1]);
            updateRegister(dest,line+1);
            gotoLine = op1+1;
            return;
        }
        if (data.toLowerCase().contains("ret "))
        {
            data = data.replaceAll("(?i)ret ", "");
            data = data.replace(" ", "");
            data = data.replace("(", "");
            String[] args = data.split("\\)");

            if (args.length < 2)
            {
                setErrorProtocol(1,line,new String[]{"2", Integer.toString(args.length)});
                return;
            }
            int op1 = StringToVal(args[0]);
            int offset = Integer.parseInt(args[1]);
            gotoLine = op1+offset+1;
            return;
        }
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
            if (!data.trim().isEmpty()) setErrorProtocol(2,-1,new String[]{data});
        }

    }

    public static void updateRegister(int index, int value)
    {
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
        String[] keys = dict.keySet().toArray(new String[0]);
        for (String key : keys)
        {
            if (key.equals(s))
            {
                return dict.get(key);
            }
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
            case 4:
                e+= "Program does not return from main";
                break;
            default:
                e += "Unkown Error";
                break;
        }

        errorString = e;
    }

}
