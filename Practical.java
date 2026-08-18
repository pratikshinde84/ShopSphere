import java.util.*;
class Practical{
    static String oneTimePad(String plainText,String key){
        StringBuilder cipherText=new StringBuilder();
        for(int i=0;i<plainText.length();i++){   
            int p=plainText.charAt(i)-'A';
            int k=key.charAt(i)-'A';
            char c=(char)('A'+((p+k)%26));   
            cipherText.append(c); 
        } 
        System.out.println("Cipher Text: "+cipherText.toString());
        return cipherText.toString();

        
    }     
    static void decrypt(String plainText,String key){
        StringBuilder plain=new StringBuilder();
        for(int i=0;i<plainText.length();i++){
            int c=plainText.charAt(i)-'A';
            int k=key.charAt(i)-'A';
            char p=(char)('A'+((c-k+26)%26));
            plain.append(p);
        }
        System.out.println("Decrypted Text: "+plain.toString());
    }
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        
        String plainText=sc.nextLine();
        String key=sc.nextLine();

        String cipher=oneTimePad(plainText,key);
        decrypt(cipher, key);
    }
}