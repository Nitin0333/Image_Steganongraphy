import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Scanner;

import javax.imageio.ImageIO;

public class Main {
    // Method to decrypt character with a key
    public static String convtstring(StringBuilder str, StringBuilder Key) {
        int num = 0;
        int key = 0;

        str = str.reverse();
        Key = Key.reverse();
        for (int i = 0; i < 8; i++) {
            if (str.charAt(i) == '1') {
                num |= (1 << i);
            }
            if (Key.charAt(i) == '1') {
                key |= (1 << i);
            } 
        }

        num ^= key;

        return Character.toString((char)num);
    }

    // Converting integer to binary string of length 8
    public static String Binary256(int num) {
        StringBuilder res = new StringBuilder();

        while (num > 0) {
            if (num % 2 == 1) {
                res.append('1');
            } else {
                res.append('0');
            }

            num >>= 1;
        }

        while (res.length() < 8) {
            res.append('0');
        }

        res = res.reverse();
        return res.toString();
    }

    // To Hide the given message in given image
    public static void HideImage(BufferedImage image, String str) {
        // Adding extra characters to identify the String
        StringBuilder mssg = new StringBuilder("nstart ");
        mssg.append(str);
        mssg.append(" nend ");

        StringBuilder encrypt = new StringBuilder();
        int min = 0;
        int max = 127;

        int ln = mssg.length();

        // Converting the characters of message and there keys into binary string
        for (int i = 0; i < ln; i++) {
            char ch = mssg.charAt(i);
            
            // Generating random key for each character and encrypting the character with that key 
            int num = (int)ch;
            int key = (int) (Math.random() * (max - min + 1)) + min;

            num ^= key;

            encrypt.append(Binary256(num));
            encrypt.append(Binary256(key));
        }

        int n = image.getWidth();
        int m = image.getHeight();
        int ptr = 0;
        ln = encrypt.length();

        // Writing the binary string to the LSB of the image pixels
        outerloop : for (int i = 0; i < n; i++) { 
            for (int j = 0; j < m; j++) {
                if (ptr >= ln) {
                    break outerloop;
                }

                Color color = new Color(image.getRGB(i, j));
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();

                if (encrypt.charAt(ptr) == '1') {
                    red |= 1;
                } else if (red % 2 == 1) {
                    red ^= 1;
                }
                ptr++;

                if (ptr < ln && encrypt.charAt(ptr) == '1') {
                    green |= 1;
                } else if (ptr < ln && green % 2 == 1) {
                    green ^= 1;
                }
                ptr++;

                if (ptr < ln && encrypt.charAt(ptr) == '1') {
                    blue |= 1;
                } else if (ptr < ln && blue % 2 == 1) {
                    blue ^= 1;
                }
                ptr++;

                color = new Color(red, green, blue);
                image.setRGB(i, j, color.getRGB());
            }
        }

        // Saving the Image
        try {
            File file = new File("hid.png");
            ImageIO.write(image, "png", file);
            System.out.println("Successfully hid the data");

        } catch (Exception e) {     // If there is an error while saving the Image
            System.out.println(e);
        }
    }

    // Extracting the message from the given input
    public static void ShowImage(BufferedImage image) {
        StringBuilder rawdata = new StringBuilder();
        int n = image.getWidth();
        int m = image.getHeight();

        // Getting LSB of all the pixels of the image
        for (int i = 0; i < n; i++)  {
            for (int j = 0; j < m; j++) {
                Color color = new Color(image.getRGB(i, j));
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();

                if (red % 2 == 1) {
                    rawdata.append('1');
                } else {
                    rawdata.append('0');
                }

                if (green % 2 == 1) {
                    rawdata.append('1');
                } else {
                    rawdata.append('0');
                }

                if (blue % 2 == 1) {
                    rawdata.append('1');
                } else {
                    rawdata.append('0');
                }
            }
        }

        int ln = rawdata.length();
        int mx = 16 * (ln / 16);

        StringBuilder decrypt = new StringBuilder();
        StringBuilder str = new StringBuilder();

        // Two flags to know if the message was successfully extracted or not
        Boolean start = true;
        Boolean end = false;

        for (int i = 0; i < mx; i += 16) {
            StringBuilder cur = new StringBuilder();
            StringBuilder key = new StringBuilder();
            
            // Dividing the string in two strings of length 16
            // First 8 characters for encrypted data
            // Last 8 characters for the key
            for (int j = i, k = i + 8; j < i + 8; j++, k++) {
                cur.append(rawdata.charAt(j));
                key.append(rawdata.charAt(k));
            }

            String chr = convtstring(cur, key);

            // If the length of a word exeed the permitted length
            if (str.length() > 100) {
                break;
            }

            // Encountering a space
            if (chr.equals(" ")) {

                // If this is start of the message
                if (start) {
                    // Checking if the message starts with nstart
                    if (str.toString().equals("nstart")) {
                        str = new StringBuilder();
                        start = false;
                    } else {            // otherwise there is no message hidden
                        break;
                    }
                } else if (str.toString().equals("nend")) {         // Checking if this is the end by comparing with nend;
                    end = true;
                    break;
                } else {                    // It is neither the start not the end of message
                    decrypt.append(str);
                    str = new StringBuilder();
                    decrypt.append(chr);
                }
            } else {                        // Just a simple character
                str.append(chr);
            }
        }

        // Output according if the message was found or not
        if (start == true || end == false) {
            System.out.println("No message");
        } else {
            System.out.println("Hidden data is : " + decrypt.toString());
        }
    }

    public static void main(String []args) {
        BufferedImage image = null;

        Scanner sc = new Scanner(System.in);
        
        // Main program Loop
        while (true) {
            System.out.println("What do you want to do?");
            System.out.println("1. Hide the data in image");
            System.out.println("2. To unhide data from image");
            System.out.println("Any other number to exit");

            int inp = sc.nextInt();

            if (inp == 1) {
                System.out.println("Enter the message you want to hide");
                sc.nextLine();
                String str = sc.nextLine();

                try {
                    File file = new File("image.png");
                    image = ImageIO.read(file);
                    HideImage(image, str);
                } catch (Exception e) {
                    System.out.println(e);
                }

            } else if (inp == 2) {

                try {
                    File file = new File("hid.png");
                    image = ImageIO.read(file);
                    ShowImage(image);
                } catch (Exception e) {
                    System.out.println(e);
                }

            } else {
                break;
            }
        }

        sc.close();
    }
}