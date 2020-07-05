package io.melvinjones;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class s3TestWrapper implements TestRule {

    private AmazonS3 s3;

    @Override
    public Statement apply(Statement base, Description description) {
        return null;
    }

//    public s3TestWrapper(AmazonS3 s3) {
//        this.s3 = s3;
//    }

    public AmazonS3 getS3() {
        return s3;
    }

    public void setS3(AmazonS3 s3) {
        this.s3 = s3;
    }
}
