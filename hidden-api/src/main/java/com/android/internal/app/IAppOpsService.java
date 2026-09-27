package com.android.internal.app;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/** Compile-only framework stub. Android provides the implementation. */
public interface IAppOpsService extends IInterface {
    void setMode(int code, int uid, String packageName, int mode) throws RemoteException;

    abstract class Stub extends Binder implements IAppOpsService {
        public static IAppOpsService asInterface(IBinder binder) {
            throw new UnsupportedOperationException();
        }
    }
}
