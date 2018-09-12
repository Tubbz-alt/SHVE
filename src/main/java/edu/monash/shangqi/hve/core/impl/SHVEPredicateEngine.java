package edu.monash.shangqi.hve.core.impl;

import edu.monash.shangqi.hve.core.PredicateOnlyAESSymmetricBlockCipher;
import edu.monash.shangqi.hve.param.SHVEKeyParameter;
import edu.monash.shangqi.hve.param.impl.SHVEEncryptionParameter;
import edu.monash.shangqi.hve.param.impl.SHVEMasterSecretKeyParameter;
import edu.monash.shangqi.hve.param.impl.SHVESecretKeyParameter;
import edu.monash.shangqi.hve.util.AESUtil;

import java.util.ArrayList;
import java.util.List;

public class SHVEPredicateEngine
        extends PredicateOnlyAESSymmetricBlockCipher {

    private long size;

    public SHVEPredicateEngine() {
    }

    public void initialize() {
        if (this.forEncryption) {
            if (!(this.key instanceof SHVEEncryptionParameter)) {
                throw new IllegalArgumentException("SHVEEncryptionParameter are required for encryption.");
            }
        } else if(!(this.key instanceof SHVESecretKeyParameter)) {
            throw new IllegalArgumentException("SHVESecretKeyParameter are required for decryption.");
        }

        SHVEKeyParameter hveKey = (SHVEKeyParameter) this.key;
        this.size = hveKey.getParameter().getSize();
    }

    public List<byte[]> process(List<byte[]> C, int inOff, int inLen) {
        if (this.key instanceof SHVESecretKeyParameter) {   // evaluation
            List<byte[]> res = new ArrayList<>();
            SHVESecretKeyParameter secretKey = (SHVESecretKeyParameter)this.key;

            byte[] z = secretKey.getD0();
            for(int i = 0; i < secretKey.getParameter().getSize(); ++i) {
                if (!secretKey.isStar(i)) {
                    byte[] c = C.get(i);
                    // use xor to predicate
                    for (int j = 0; j < z.length; j++) {
                        z[j] ^= c[j];
                    }
                }
            }
            try {
                AESUtil.decrypt(secretKey.getD1(), z);
                res.add(new byte[]{1});
            } catch (RuntimeException e) {
                // if the key is not correct, the given pattern is not matched
                res.add(new byte[]{0});
            }
           return res;

        } else if (inLen <= this.inBytes && inLen >= this.inBytes) {    // encryption
            SHVEEncryptionParameter encParams = (SHVEEncryptionParameter)this.key;
            if(encParams.isMaster()) {
                SHVEMasterSecretKeyParameter pk = encParams.getMasterSecretKey();
                C = new ArrayList<>();
                for (int i = 0; i < this.size; ++i) {
                    int j = encParams.getAttributeAt(i);
                    C.add(AESUtil.encode(String.valueOf(j)
                            .concat(String.valueOf(i)).getBytes(), pk.getMSK()));
                }
                return C;
            }
        }
        return null;
    }
}
