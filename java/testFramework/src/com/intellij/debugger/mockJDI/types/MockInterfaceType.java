/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.debugger.mockJDI.types;

import com.sun.jdi.InterfaceType;
import com.sun.jdi.ClassType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ObjectReference;
import com.intellij.debugger.mockJDI.MockVirtualMachine;

import java.util.List;
import java.util.ArrayList;

public class MockInterfaceType extends MockReferenceType implements InterfaceType {
  public MockInterfaceType(final MockVirtualMachine virtualMachine, Class type) {
    super(type, virtualMachine);
  }

  @Override
  protected List<ReferenceType> getThisAndAllSupers() {
    ArrayList<ReferenceType> referenceTypes = new ArrayList<>();
    referenceTypes.add(this);
    referenceTypes.addAll(allInterfaces());
    return referenceTypes;
  }

  @Override
  public List<InterfaceType> superinterfaces() {
    ArrayList<InterfaceType> interfaceTypes = new ArrayList<>();
    for (Class aClass : myType.getInterfaces()) {
      interfaceTypes.add(myVirtualMachine.createInterfaceType(aClass));
    }
    return interfaceTypes;
  }

  @Override
  public List<InterfaceType> subinterfaces() {
    throw new UnsupportedOperationException("Not implemented: \"subinterfaces\" in " + getClass().getName());
  }

  @Override
  public List<ClassType> implementors() {
    throw new UnsupportedOperationException("Not implemented: \"implementors\" in " + getClass().getName());
  }

  @Override
  public List<ObjectReference> instances(long l) {
    throw new UnsupportedOperationException("Not implemented: \"instances\" in " + getClass().getName());
  }

  @Override
  public int majorVersion() {
    throw new UnsupportedOperationException("Not implemented: \"majorVersion\" in " + getClass().getName());
  }

  @Override
  public int minorVersion() {
    throw new UnsupportedOperationException("Not implemented: \"minorVersion\" in " + getClass().getName());
  }

  @Override
  public int constantPoolCount() {
    throw new UnsupportedOperationException("Not implemented: \"constantPoolCount\" in " + getClass().getName());
  }

  @Override
  public byte[] constantPool() {
    throw new UnsupportedOperationException("Not implemented: \"constantPool\" in " + getClass().getName());
  }
}
