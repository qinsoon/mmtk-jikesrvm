/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */
package org.jikesrvm.mm.mminterface;

import org.jikesrvm.VM;
import org.mmtk.plan.Plan;
import org.mmtk.plan.nogc.NoGCMutator;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;
import org.jikesrvm.runtime.Magic;

import static org.jikesrvm.runtime.EntrypointHelper.getField;
import static org.jikesrvm.runtime.SysCall.sysCall;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_WORD;

@Uninterruptible
public class NoGCContext extends NoGCMutator {
    // NoGC BumpAllocator
    @Entrypoint
    Address threadId;
    @Entrypoint
    Address cursor;
    @Entrypoint
    Address limit;
    @Entrypoint
    Address space;
    @Entrypoint
    Address spaceFat; // space is a fat pointer
    @Entrypoint
    Address planNoGC;
    // CommonMutatorContext
    // Immortal BumpAllocator
    @Entrypoint
    Address threadIdImmortal;
    @Entrypoint
    Address cursorImmortal;
    @Entrypoint
    Address limitImmortal;
    @Entrypoint
    Address spaceImmortal;
    @Entrypoint
    Address spaceImmortalFat;
    @Entrypoint
    Address planImmortal;
    // LargeObjectAllocator
    @Entrypoint
    Address threadIdLos;
    @Entrypoint
    Address spaceLos;
    @Entrypoint
    Address planLos;

    static final Offset threadIdOffset = getField(NoGCContext.class, "threadId", Address.class).getOffset();
    static final Offset cursorOffset = getField(NoGCContext.class, "cursor", Address.class).getOffset();
    static final Offset limitOffset = getField(NoGCContext.class, "limit", Address.class).getOffset();
    static final Offset spaceOffset = getField(NoGCContext.class, "space", Address.class).getOffset();
    static final Offset spaceFatOffset = getField(NoGCContext.class, "spaceFat", Address.class).getOffset();
    static final Offset planNoGCOffset = getField(NoGCContext.class, "planNoGC", Address.class).getOffset();

    static final Offset threadIdImmortalOffset = getField(NoGCContext.class, "threadIdImmortal", Address.class).getOffset();
    static final Offset cursorImmortalOffset = getField(NoGCContext.class, "cursorImmortal", Address.class).getOffset();
    static final Offset limitImmortalOffset = getField(NoGCContext.class, "limitImmortal", Address.class).getOffset();
    static final Offset spaceImmortalOffset = getField(NoGCContext.class, "spaceImmortal", Address.class).getOffset();
    static final Offset spaceImmortalFatOffset = getField(NoGCContext.class, "spaceImmortalFat", Address.class).getOffset();
    static final Offset planImmortalOffset = getField(NoGCContext.class, "planImmortal", Address.class).getOffset();

    static final Offset threadIdLosOffset = getField(NoGCContext.class, "threadIdLos", Address.class).getOffset();
    static final Offset spaceLosOffset = getField(NoGCContext.class, "spaceLos", Address.class).getOffset();
    static final Offset planLosOffset = getField(NoGCContext.class, "planLos", Address.class).getOffset();

    @Override
    public Address alloc(int bytes, int align, int offset, int allocator, int site) {
        if (allocator == Plan.ALLOC_DEFAULT) {
            // Align allocation
            Word mask = Word.fromIntSignExtend(align - 1);
            Word negOff = Word.fromIntSignExtend(-offset);

            Offset delta = negOff.minus(cursor.toWord()).and(mask).toOffset();

            Address result = cursor.plus(delta);

            Address newCursor = result.plus(bytes);

            if (newCursor.GT(limit)) {
                Address handle = Magic.objectAsAddress(this).plus(threadIdOffset);
                return sysCall.sysAllocSlowBumpMonotoneImmortal(handle, bytes, align, offset, allocator);
            } else {
                cursor = newCursor;
                return result;
            }
        } else {
            Address handle = Magic.objectAsAddress(this).plus(threadIdOffset);
            return sysCall.sysAlloc(handle, bytes, align, offset, allocator);
        }
    }

    public Address setBlock(Address mmtkHandle) {
        threadId = mmtkHandle.loadAddress();
        cursor   = mmtkHandle.plus(BYTES_IN_WORD).loadAddress();
        limit    = mmtkHandle.plus(BYTES_IN_WORD * 2).loadAddress();
        space    = mmtkHandle.plus(BYTES_IN_WORD * 3).loadAddress();
        spaceFat = mmtkHandle.plus(BYTES_IN_WORD * 4).loadAddress();
        planNoGC = mmtkHandle.plus(BYTES_IN_WORD * 5).loadAddress();

        threadIdImmortal = mmtkHandle.plus(BYTES_IN_WORD * 6).loadAddress();
        cursorImmortal = mmtkHandle.plus(BYTES_IN_WORD * 7).loadAddress();
        limitImmortal = mmtkHandle.plus(BYTES_IN_WORD * 8).loadAddress();
        spaceImmortal = mmtkHandle.plus(BYTES_IN_WORD * 9).loadAddress();
        spaceImmortalFat = mmtkHandle.plus(BYTES_IN_WORD * 10).loadAddress();
        planImmortal = mmtkHandle.plus(BYTES_IN_WORD * 11).loadAddress();

        threadIdLos = mmtkHandle.plus(BYTES_IN_WORD * 12).loadAddress();
        spaceLos = mmtkHandle.plus(BYTES_IN_WORD * 13).loadAddress();
        planLos = mmtkHandle.plus(BYTES_IN_WORD * 14).loadAddress();

        return Magic.objectAsAddress(this).plus(threadIdOffset);
    }

    @Override
    public void postAlloc(ObjectReference ref, ObjectReference typeRef,
                          int bytes, int allocator) {
        Address handle = Magic.objectAsAddress(this).plus(threadIdOffset);
        sysCall.sysPostAlloc(handle, ref, typeRef, bytes, allocator);
    }
}
