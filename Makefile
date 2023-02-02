# JNI header files
JAVA_HOME = %JAVA_HOME%
JNI_INCLUDE = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/win32

# Additional include paths
INCLUDES = -Itarget/generated-headers

# Source files
SOURCES = IDirect3DDevice9Wrapper.cpp

# Object files
OBJECTS = $(addprefix target/build/, $(SOURCES:.cpp=.o))

LIBRARY = src/main/resources/jfxshader.dll

all: create-dirs $(LIBRARY)

$(LIBRARY): $(OBJECTS)
	$(CXX) -shared -o $@ $^ -Wl,--add-stdcall-alias

target/build/%.o: src/main/native-d3d/%.cpp
	$(CXX) $(CFLAGS) -c $(JNI_INCLUDE) $(INCLUDES) -o $@ $<

clean:
	rm -f target/build/$(OBJECTS) $(LIBRARY)

create-dirs:
	mkdir -p target/build
