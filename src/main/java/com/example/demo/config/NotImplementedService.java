package com.example.demo.config;

import com.example.demo.exception.ApiException;
import java.lang.reflect.Proxy;
import org.springframework.http.HttpStatus;

/**
 * สร้าง Service ชั่วคราวที่ทุกเมธอดตอบ 501 "Not implemented yet"
 * ใช้ใน Default*ServiceConfig เพื่อให้แอปรันได้ก่อนที่ Service จริงจะเสร็จ
 */
public final class NotImplementedService {

    private NotImplementedService() {
    }

    public static <T> T of(Class<T> type) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (instance, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        switch (method.getName()) {
                            case "toString":
                                return type.getSimpleName() + " (not implemented)";
                            case "hashCode":
                                return System.identityHashCode(instance);
                            case "equals":
                                return instance == args[0];
                            default:
                                return null;
                        }
                    }
                    throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet", null);
                });
        return type.cast(proxy);
    }
}
