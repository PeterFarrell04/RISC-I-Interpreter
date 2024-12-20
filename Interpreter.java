import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Interpreter
{
    //options for calling convention
    static int globalUpTo = 9;
    static int childParamsUpTo = 15;
    static int localVarsUpTo = 25;
    static int parentParamsUpTo = 31;

    //global variables
    static boolean returnOnErrors = false;
    static int errorFlag = -1;
    static String errorString = "";
    static int [] reg;

    static int regBankIndex = 0;
    static HashMap<String, Integer> dict = new HashMap<String, Integer>();
    static int gotoLine;
    static boolean mainFound = false;
    static boolean finalLine = false;
    static int line = 0;
    static int errorCount = 0;
    public static void main(String[] args) {
        File file = new File("input.txt"); //to be replaced with user input

        setup();

        System.out.println("---START OF PROGRAM---\n");
        parse(file,line);
        System.out.println("\n---END OF PROGRAM---");

        /*
        updateRegister(15,100);
        regBankIndex++;
        */

        debugPrintRegisters(0, 137);

        System.out.println("Total Errors: " + errorCount);

    }

    public static void setup()
    {
        reg = new int[138];
        reg[0] = 0;
        reg[137] = -10;
        gotoLine = -1;
        mainFound = false;
        errorFlag = -1;
        errorCount = 0;
    }

    public static void parse(File f,int lineStart)
    {
        //if (mainFound) debugPrintRegisters(1,1);
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

            //System.out.println(data);
            if (errorFlag != -1)
            {
                System.out.println(errorString);
                if (returnOnErrors) return;
            }
            if (s.hasNextLine() && !finalLine)
            {
                if (gotoLine < -1) finalLine = true;

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
            regBankIndex++;
            System.out.println(regBankIndex);
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
            if (regBankIndex > 0) regBankIndex--;
            System.out.println(regBankIndex);
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
            int op1 = getRegisterContents(getRegisterIndexFromString(args[0]));
            int op2 = StringToVal(args[1]);
            int destIndex = getRegisterIndexFromString(args[2]);
            updateRegister(destIndex,op1+op2);
            return;
        }

        if (!data.isEmpty()) {
            if (!data.trim().isEmpty()) setErrorProtocol(2,line,new String[]{data});
        }
        if (finalLine) setErrorProtocol(5,line,new String[]{data});

    }

    public static int getRegisterContents(int index)
    {
        if (index <= globalUpTo) {
            return reg[index];
        }else {
            return reg[(16*(8-regBankIndex)) - (31-index)+9];
        }
    }

    public static void updateRegister(int index, int value)
    {
        if (index > 0)
        {
            if (index <= globalUpTo) {
                reg[index] = value;
            }else {
                reg[(16*(8-regBankIndex)) - (31-index)+9] = value;
            }
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
            return getRegisterContents(register);
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
        setErrorProtocol(2,-1,new String[]{s});
        return -1000; //replace with proper error handling
    }

    public static void debugPrintRegisters(int start,int end)
    {
        char rank = 65;
        int count = 31;
        for (int i = end; i > start; i--)
        {
            String type = "";
            if (i < 10) type = "Global";
            if (i >= 10)
            {
                type = rank+":"+count;
                if (count <= 15)
                {
                    char next = (char) (rank+1);
                    type = next + ":" + (count+16) + "&" + type;
                }
                count--;
                if (count < 10)
                {
                    rank++;
                    count = 25;
                }
            }



            System.out.println("("+type+"):"+"R" + i + " = " + reg[i] + ",");
        }
    }

    public static void setErrorProtocol(int flag, int line,String[] args)
    {
        errorCount++;
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
            case 5:
                e+= "No delay slot / instruction after final return";
                break;
            default:
                e += "Unkown Error";
                break;
        }

        errorString = e;
    }

}
