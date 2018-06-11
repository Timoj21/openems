package io.openems.edge.core.sum.internal;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.sum.Sum;

public abstract class ChannelsFunction<C extends OpenemsComponent, T> {

	private final Logger log = LoggerFactory.getLogger(ChannelsFunction.class);
	private final Channel<T> targetChannel;
	private final ChannelId sourceChannelId;

	protected boolean debug = false;

	protected final Map<String, Value<T>> valueMap = new ConcurrentHashMap<>();

	public ChannelsFunction(Sum parent, io.openems.edge.core.sum.Sum.ChannelId targetChannelId,
			ChannelId sourceChannelId) {
		this.targetChannel = parent.channel(targetChannelId);
		this.sourceChannelId = sourceChannelId;
	}

	public void addComponent(C component) {
		if (this.debug) {
			log.info("Add Component [" + component.id() + "] of type [" + component.getClass().getSimpleName() + "]");
		}
		final Consumer<Value<T>> handler = value -> {
			this.valueMap.put(component.id(), value);
			try {
				this.targetChannel.setNextValue(this.calculate());
			} catch (NoSuchElementException e) {
				this.targetChannel.setNextValue(null);
			}
		};
		Channel<T> channel = component.channel(this.sourceChannelId);
		handler.accept(channel.getNextValue()); // handle current value
		channel.onSetNextValue(handler); // and every upcoming value
	}

	public void removeComponent(OpenemsComponent component) {
		if (this.debug) {
			log.info(
					"Remove Component [" + component.id() + "] of type [" + component.getClass().getSimpleName() + "]");
		}
		this.valueMap.remove(component.id());
	}

	public ChannelsFunction<C, T> debug() {
		this.debug = true;
		return this;
	}

	protected abstract double calculate() throws NoSuchElementException;
}
