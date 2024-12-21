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
    static boolean zeroFlag = false;
    static boolean carryFlag = false;
    static boolean overflowFlag = false;
    static boolean negativeFlag = false;
    static boolean returnOnErrors = false;
    static int errorFlag = -1;
    static String errorString = "";
    static int [] reg;

    static int regBankIndex = 0;
    static int regBankBufferInc = 0;
    static int regBankBufferDec = 0;
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


        debugPrintRegisters(0, 137);

        System.out.println("Total Errors: " + errorCount);
        debugPrintFlags();

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
                else
                {
                    regBankIndex += regBankBufferInc - regBankBufferDec;
                    regBankBufferDec = 0;
                    regBankBufferInc = 0;
                    parse(f,nl);
                }
            }
            else
            {

                if (!mainFound)
                {
                    mainFound = true;
                    //String[] a = dict.keySet().toArray(new String[0]);
                    //for (String st : a) System.out.println(st);
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
        data = data.strip();
        if (data.contains(";"))
        {
            data = data.substring(0,data.indexOf(';'));
        }
        if (data.contains(":"))
        {
            data = data.replace(":","");
            if (dict.containsKey(data))
            {
                if (dict.get(data) != line) {
                    setErrorProtocol(0, line, new String[]{data});
                }
            }else
            {
                dict.put(data,line);
            }
            return;
        }
        if (!mainFound) return;
        if (data.toLowerCase().startsWith("callr "))
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
            regBankBufferInc++;
            gotoLine = op1+1;
            return;
        }

        if (data.toLowerCase().startsWith("j"))
        {
            data = data.substring(1);
            String[] args = data.split(" ");
            //for (String s : args) System.out.println(s);
            int jumpTo = 0;
            if (args.length < 2)
            {
                setErrorProtocol(1,line,new String[]{"2", Integer.toString(args.length)});
                return;
            }
            if (dict.get(args[1]) != null)
            {
                jumpTo = dict.get(args[1]);
            }else
            {
                setErrorProtocol(6,line,new String[]{args[1]});
                return;
            }
            if (args[0].equals("eq"))
            {
                if (zeroFlag) gotoLine = jumpTo+1;
                return;
            }
            if (args[0].equals("ne"))
            {
                if (!zeroFlag) gotoLine = jumpTo+1;
                return;
            }
            if (args[0].equals("lt"))
            {
                if (negativeFlag) gotoLine = jumpTo+1;
                return;
            }
            if (args[0].equals("le"))
            {
                if (negativeFlag || zeroFlag) gotoLine = jumpTo+1;
                return;
            }
            if (args[0].equals("gt"))
            {
                if (!negativeFlag) gotoLine = jumpTo+1;
                return;
            }
            if (args[0].equals("ge"))
            {
                if ((!negativeFlag) || zeroFlag) gotoLine = jumpTo+1;
                return;
            }
            if (args[0].equals("mp"))
            {
                gotoLine = jumpTo+1;
                return;
            }

            setErrorProtocol(7,line,new String[]{args[0]});
            return;

        }

        if (data.toLowerCase().startsWith("ret "))
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
            if (regBankIndex > 0) regBankBufferDec++;
            //System.out.println(regBankIndex);
            gotoLine = op1+offset+1;
            return;
        }
        //standard operators
        if (applyOperator(data,line,"add ","+")) return;
        if (applyOperator(data,line,"sub ","-")) return;
        if (applyOperator(data,line,"xor ","^")) return;
        if (applyOperator(data,line,"and ","&")) return;
        if (applyOperator(data,line,"or ","|")) return;
        if (applyOperator(data,line,"sll ","<<")) return;
        if (applyOperator(data,line,"slr ",">>")) return;




        if (!data.isEmpty()) {
            if (!data.trim().isEmpty()) setErrorProtocol(2,line,new String[]{data});
        }
        if (finalLine) setErrorProtocol(5,line,new String[]{data});

    }

    public static boolean applyOperator(String data,int line,String token, String operator)
    {

        if (data.toLowerCase().startsWith(token)) {
            data = data.replaceAll("(?i)" + token, "");
            data = data.replace(" ", "");
            String[] args = data.split(",");
            if (args.length < 3) {
                setErrorProtocol(1, line, new String[]{"3", Integer.toString(args.length)});
                return true;
            }
            int op1 = getRegisterContents(getRegisterIndexFromString(args[0]));
            int op2 = StringToVal(args[1]);
            int destIndex = getRegisterIndexFromString(args[2]);
            int result = 0;
            switch (operator) {
                case "+":
                    result = op1 + op2;
                    break;
                case "-":
                    result = op1 - op2;
                    break;
                case "^":
                    result = op1 ^ op2;
                    break;
                case "|":
                    result = op1 | op2;
                    break;
                case "&":
                    result = op1 & op2;
                    break;
                case "<<":
                    result = op1 << op2;
                    break;
                case ">>":
                    result = op1 >> op2;
                    break;


            }
            if (args.length > 3)
            {
                if (args[3].equals("{C}"))
                {
                    zeroFlag = (result==0);
                    negativeFlag = (result<0);
                    carryFlag = (op1 >= op2);
                    overflowFlag = ((Math.signum(op1) != Math.signum(op2)) && (Math.signum(op1) != Math.signum(result)));
                }
            }
            updateRegister(destIndex,result);
            return true;
        }else
        {
            return false;
        }
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
            case 6:
                e+= "Tag \""+ args[0] + "\" does not exist in the program";
                break;
            case 7:
                e+= "\"j"+ args[0] + "\" is not a valid jump condition";
                break;
            default:
                e += "Unkown Error";
                break;
        }

        errorString = e;
    }

    public static void debugPrintRegisters(int start,int end)
    {
        char rank = 65;
        int count = 31;
        for (int i = end; i >= start; i--)
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

    public static void debugPrintFlags()
    {
        System.out.printf("Zero: %b, Negative: %b, Carry: %b, Overflow: %b",zeroFlag,negativeFlag,carryFlag,overflowFlag);
    }

}
