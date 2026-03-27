import com.erwin.backend.service.BackupEncryptionUtil;

public class TestEncrypt {

    public static void main(String[] args) {

        BackupEncryptionUtil util = new BackupEncryptionUtil();

        // poner la clave manualmente
        util.setKey("GjbanRX68SF/zxArHhRMCdg87QWQCRTSFXS5jaPhbp4=");

        String encrypted = util.encrypt("12345");

        System.out.println(encrypted);
    }

}