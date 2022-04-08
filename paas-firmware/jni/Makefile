JAVA_HOME = ${HOME}/work/tools/java/jdk1.8.0_202
CC = g++
FLAGS = -D_REENTRANT -fPIC
INCLUDE = -I . -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux/

all: libfirmwarejni.so firmware_tool

install:
	cp libfirmwarejni.so lib

firmware.o: firmware.c
	$(CC) $(FLAGS) $(INCLUDE) -c $< -o $@

libfirmwarejni.so: firmware.o
	$(CC) -shared -fPIC -o $@ $<
	-cp libfirmwarejni.so ../src/main/java/com/wlwx/paas/underlying/firmware

firmware_tool:firmware_tool.o
	$(CC) $(FLAGS) -static -o $@ firmware_tool.o $(LIB) $(LIBRARY)

firmware_collect:firmware_collect.o
    $(CC) $(FLAGS) -static -o $@ firmware_collect.o $(LIB) $(LIBRARY)

.PHONY : clean
clean:
	rm -f *.o libfirmwarejni.so firmware_tool
