/*
 *
 * Copyright 2017-2018 549477611@qq.com(xiaoyu)
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.raincat.core.service.impl;

import com.raincat.common.enums.PropagationEnum;
import com.raincat.core.annotation.TxTransaction;
import com.raincat.common.bean.TransactionInvocation;
import com.raincat.common.bean.TxTransactionInfo;
import com.raincat.core.concurrent.threadlocal.CompensationLocal;
import com.raincat.core.helper.SpringBeanUtils;
import com.raincat.core.service.AspectTransactionService;
import com.raincat.core.service.TxTransactionFactoryService;
import com.raincat.core.service.TxTransactionHandler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

/**
 * @author xiaoyu
 */
@Service
public class AspectTransactionServiceImpl implements AspectTransactionService {

    private final TxTransactionFactoryService txTransactionFactoryService;

    @Autowired
    public AspectTransactionServiceImpl(TxTransactionFactoryService txTransactionFactoryService) {
        this.txTransactionFactoryService = txTransactionFactoryService;
    }

    @Override
    public Object invoke(String transactionGroupId, ProceedingJoinPoint point) throws Throwable {
        /*
         获得事务分组id后，开始执行切面方法
         */
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> clazz = point.getTarget().getClass();
        Object[] args = point.getArgs();
        Method thisMethod = clazz.getMethod(method.getName(), method.getParameterTypes());

        final String compensationId = CompensationLocal.getInstance().getCompensationId();

        final TxTransaction txTransaction = method.getAnnotation(TxTransaction.class);

        final int waitMaxTime = txTransaction.waitMaxTime();

        final PropagationEnum propagation = txTransaction.propagation();

        TransactionInvocation invocation = new TransactionInvocation(clazz, thisMethod.getName(), args, method.getParameterTypes());
        TxTransactionInfo info = new TxTransactionInfo(invocation,transactionGroupId,compensationId,waitMaxTime,propagation);

        /*
        根据TxTransactionInfo的数据，获取不同的actionHandler。有的actionHandler是创建
         */
        final Class c = txTransactionFactoryService.factoryOf(info);
        final TxTransactionHandler txTransactionHandler =
                (TxTransactionHandler) SpringBeanUtils.getInstance().getBean(c);
        return txTransactionHandler.handler(point, info);
    }
}
