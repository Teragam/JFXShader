#include "de_teragam_jfxshader_internal_d3d_IDirect3DDevice9.h"
#include <d3d9.h>
#include <stdio.h>

jfieldID deviceField = NULL;
jfieldID resultCodeField = NULL;

IDirect3DDevice9* getDevice(JNIEnv *env, jobject obj) {
    if (deviceField == NULL) {
        deviceField = env->GetFieldID(env->GetObjectClass(obj), "handle", "J");
    }
    return (IDirect3DDevice9*) env->GetLongField(obj, deviceField);
}

void setResultCode(JNIEnv *env, jobject obj, HRESULT resultCode) {
    if (resultCodeField == NULL) {
        resultCodeField = env->GetFieldID(env->GetObjectClass(obj), "resultCode", "I");
    }
    env->SetIntField(obj, resultCodeField, resultCode);
}

JNIEXPORT void JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_setFVF(JNIEnv *env, jobject obj, jint fvf) {
    setResultCode(env, obj, getDevice(env, obj)->SetFVF(fvf));
}

JNIEXPORT void JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_setVertexShader(JNIEnv *env, jobject obj, jlong pShader) {
    setResultCode(env, obj, getDevice(env, obj)->SetVertexShader((IDirect3DVertexShader9*) pShader));
}

JNIEXPORT jlong JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_createVertexShader(JNIEnv *env, jobject obj, jbyteArray pFunction) {
    IDirect3DVertexShader9* pShader = NULL;
    jbyte* pFunctionBytes = env->GetByteArrayElements(pFunction, NULL);
    setResultCode(env, obj, getDevice(env, obj)->CreateVertexShader((DWORD*) pFunctionBytes, &pShader));
    env->ReleaseByteArrayElements(pFunction, pFunctionBytes, JNI_ABORT);
    return (jlong) pShader;
}

JNIEXPORT void JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_setVertexShaderConstantF(JNIEnv *env, jobject obj, jint startRegister, jfloatArray pConstantData, jint vector4fCount) {
    jfloat* pConstantDataFloats = env->GetFloatArrayElements(pConstantData, NULL);
    setResultCode(env, obj, getDevice(env, obj)->SetVertexShaderConstantF(startRegister, (float*) pConstantDataFloats, vector4fCount));
    env->ReleaseFloatArrayElements(pConstantData, pConstantDataFloats, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_setPixelShader(JNIEnv *env, jobject obj, jlong pShader) {
    setResultCode(env, obj, getDevice(env, obj)->SetPixelShader((IDirect3DPixelShader9*) pShader));
}

JNIEXPORT void JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_setRenderState(JNIEnv *env, jobject obj, jint state, jint value) {
    setResultCode(env, obj, getDevice(env, obj)->SetRenderState((D3DRENDERSTATETYPE) state, value));
}

JNIEXPORT jint JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_getRenderState(JNIEnv *env, jobject obj, jint state) {
    DWORD value = 0;
    setResultCode(env, obj, getDevice(env, obj)->GetRenderState((D3DRENDERSTATETYPE) state, &value));
    return value;
}

JNIEXPORT void JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_setStreamSource(JNIEnv *env, jobject obj, jint streamNumber, jlong pStreamData, jint offsetInBytes, jint stride) {
    setResultCode(env, obj, getDevice(env, obj)->SetStreamSource(streamNumber, (IDirect3DVertexBuffer9*) pStreamData, offsetInBytes, stride));
}

JNIEXPORT void JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_setIndices(JNIEnv *env, jobject obj, jlong pIndexData) {
    setResultCode(env, obj, getDevice(env, obj)->SetIndices((IDirect3DIndexBuffer9*) pIndexData));
}

JNIEXPORT void JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_drawIndexedPrimitive(JNIEnv *env, jobject obj, jint primitiveType, jint baseVertexIndex, jint minVertexIndex, jint numVertices, jint startIndex, jint primCount) {
    setResultCode(env, obj, getDevice(env, obj)->DrawIndexedPrimitive((D3DPRIMITIVETYPE) primitiveType, baseVertexIndex, minVertexIndex, numVertices, startIndex, primCount));
}

JNIEXPORT void JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_releaseResource(JNIEnv *env, jobject obj, jlong pResource) {
    if (pResource != 0) {
        setResultCode(env, obj, ((IUnknown*) pResource)->Release());
    }
}

JNIEXPORT jlong JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_createVertexBuffer(JNIEnv *env, jobject obj, jint length, jint usage, jint fvf, jint pool, jlong pSharedHandle) {
    IDirect3DVertexBuffer9* pVertexBuffer = nullptr;
    setResultCode(env, obj, getDevice(env, obj)->CreateVertexBuffer(length, usage, fvf, (D3DPOOL) pool, &pVertexBuffer, nullptr));
    return (jlong) pVertexBuffer;
}

JNIEXPORT jlong JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_createIndexBuffer(JNIEnv *env, jobject obj, jint length, jint usage, jint format, jint pool, jlong pSharedHandle) {
    IDirect3DIndexBuffer9* pIndexBuffer = NULL;
    setResultCode(env, obj, getDevice(env, obj)->CreateIndexBuffer(length, usage, (D3DFORMAT) format, (D3DPOOL) pool, &pIndexBuffer, NULL));
    return (jlong) pIndexBuffer;
}

JNIEXPORT void JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_uploadVertexBufferData(JNIEnv *env, jobject obj, jlong pVertexBuffer, jfloatArray pVertexData, jint vertexBufferLength) {
    float* vertexBuffer = (float*) (env->GetPrimitiveArrayCritical(pVertexData, 0));
    void* pLockedVertexBuffer = NULL;
    setResultCode(env, obj, ((IDirect3DVertexBuffer9*) pVertexBuffer)->Lock(0, vertexBufferLength, &pLockedVertexBuffer, 0));
    memcpy(pLockedVertexBuffer, vertexBuffer, vertexBufferLength);
    ((IDirect3DVertexBuffer9*) pVertexBuffer)->Unlock();
    env->ReleasePrimitiveArrayCritical(pVertexData, vertexBuffer, 0);
}

JNIEXPORT void JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_uploadIndexBufferDataInt(JNIEnv *env, jobject obj, jlong pIndexBuffer, jintArray pIndexData, jint indexBufferLength) {
    int* indexBuffer = (int*) (env->GetPrimitiveArrayCritical(pIndexData, 0));
    void* pLockedIndexBuffer = NULL;
    setResultCode(env, obj, ((IDirect3DIndexBuffer9*) pIndexBuffer)->Lock(0, indexBufferLength, &pLockedIndexBuffer, 0));
    memcpy(pLockedIndexBuffer, indexBuffer, indexBufferLength);
    ((IDirect3DIndexBuffer9*) pIndexBuffer)->Unlock();
    env->ReleasePrimitiveArrayCritical(pIndexData, indexBuffer, 0);
}

JNIEXPORT void JNICALL Java_de_teragam_jfxshader_internal_d3d_IDirect3DDevice9_uploadIndexBufferDataShort(JNIEnv *env, jobject obj, jlong pIndexBuffer, jshortArray pIndexData, jint indexBufferLength) {
    short* indexBuffer = (short*) (env->GetPrimitiveArrayCritical(pIndexData, 0));
    void* pLockedIndexBuffer = NULL;
    setResultCode(env, obj, ((IDirect3DIndexBuffer9*) pIndexBuffer)->Lock(0, indexBufferLength, &pLockedIndexBuffer, 0));
    memcpy(pLockedIndexBuffer, indexBuffer, indexBufferLength);
    ((IDirect3DIndexBuffer9*) pIndexBuffer)->Unlock();
    env->ReleasePrimitiveArrayCritical(pIndexData, indexBuffer, 0);
}
