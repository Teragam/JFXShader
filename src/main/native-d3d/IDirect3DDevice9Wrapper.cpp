#include "de_teragam_jfxshader_internal_IDirect3DDevice9Wrapper.h"
#include <d3d9.h>

jfieldID deviceField = NULL;

IDirect3DDevice9* getDevice(JNIEnv *env, jobject obj) {
    if (deviceField == NULL) {
        deviceField = env->GetFieldID(env->GetObjectClass(obj), "pDevice", "J");
    }
    return (IDirect3DDevice9*) env->GetLongField(obj, deviceField);
}

JNIEXPORT jint JNICALL Java_de_teragam_jfxshader_internal_IDirect3DDevice9Wrapper_setFVF(JNIEnv *env, jobject obj, jint fvf) {
    return getDevice(env, obj)->SetFVF(fvf);
}

JNIEXPORT jint JNICALL Java_de_teragam_jfxshader_internal_IDirect3DDevice9Wrapper_setVertexShader(JNIEnv *env, jobject obj, jlong pShader) {
    return getDevice(env, obj)->SetVertexShader((IDirect3DVertexShader9*) pShader);
}

JNIEXPORT jlong JNICALL Java_de_teragam_jfxshader_internal_IDirect3DDevice9Wrapper_createVertexShader(JNIEnv *env, jobject obj, jbyteArray pFunction) {
    IDirect3DVertexShader9* pShader = NULL;
    jbyte* pFunctionBytes = env->GetByteArrayElements(pFunction, NULL);
    getDevice(env, obj)->CreateVertexShader((DWORD*) pFunctionBytes, &pShader);
    env->ReleaseByteArrayElements(pFunction, pFunctionBytes, JNI_ABORT);
    return (jlong) pShader;
}

JNIEXPORT jint JNICALL Java_de_teragam_jfxshader_internal_IDirect3DDevice9Wrapper_setVertexShaderConstantF(JNIEnv *env, jobject obj, jint startRegister, jfloatArray pConstantData, jint vector4fCount) {
    jfloat* pConstantDataFloats = env->GetFloatArrayElements(pConstantData, NULL);
    jint result = getDevice(env, obj)->SetVertexShaderConstantF(startRegister, (float*) pConstantDataFloats, vector4fCount);
    env->ReleaseFloatArrayElements(pConstantData, pConstantDataFloats, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL Java_de_teragam_jfxshader_internal_IDirect3DDevice9Wrapper_setPixelShader(JNIEnv *env, jobject obj, jlong pShader) {
    return getDevice(env, obj)->SetPixelShader((IDirect3DPixelShader9*) pShader);
}
